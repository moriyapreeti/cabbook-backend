package org.cabbook.ride;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import org.cabbook.auth.AuthUser;
import org.cabbook.common.ApiException;
import org.cabbook.driver.Driver;
import org.cabbook.driver.DriverRepository;
import org.cabbook.driver.DriverService;
import org.cabbook.pricing.*;
import org.cabbook.promo.PromoService;
import org.cabbook.realtime.RealtimePublisher;
import org.cabbook.ride.RideDtos.*;
import org.cabbook.user.Role;
import org.cabbook.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RideService {
    private final RideRepository rides;
    private final UserRepository users;
    private final DriverRepository drivers;
    private final DriverService driverService;
    private final RouteProvider routes;
    private final FareCalculator fares;
    private final PromoService promos;
    private final RealtimePublisher realtime;
    private final double radiusKm;
    private final int maxDrivers;

    public RideService(RideRepository rides, UserRepository users, DriverRepository drivers, DriverService driverService,
                       RouteProvider routes, FareCalculator fares, PromoService promos, RealtimePublisher realtime,
                       @Value("${cabbook.matching.radius-km}") double radiusKm,
                       @Value("${cabbook.matching.max-drivers}") int maxDrivers) {
        this.rides = rides; this.users = users; this.drivers = drivers; this.driverService = driverService;
        this.routes = routes; this.fares = fares; this.promos = promos; this.realtime = realtime;
        this.radiusKm = radiusKm; this.maxDrivers = maxDrivers;
    }

    // ---------- rider side ----------

    public FareEstimate estimate(BookRideRequest r) {
        RouteInfo route = routes.route(r.pickupLat(), r.pickupLng(), r.dropLat(), r.dropLng());
        BigDecimal fare = fares.calculate(route.distanceKm(), route.durationMin());
        BigDecimal discount = (r.promoCode() == null || r.promoCode().isBlank())
                ? BigDecimal.ZERO : promos.discountFor(r.promoCode(), fare);
        return new FareEstimate(route.distanceKm(), route.durationMin(), fare, discount, fare.subtract(discount));
    }

    @Transactional
    public RideResponse book(AuthUser rider, BookRideRequest req) {
        if (rides.existsByRiderIdAndStatusIn(rider.id(), Set.of(RideStatus.REQUESTED, RideStatus.ACCEPTED, RideStatus.ARRIVED, RideStatus.STARTED)))
            throw ApiException.conflict("You already have an active ride");

        FareEstimate est = estimate(req);
        Ride ride = new Ride();
        ride.setRider(users.getReferenceById(rider.id()));
        ride.setPickupAddress(req.pickupAddress());
        ride.setPickupLat(req.pickupLat());
        ride.setPickupLng(req.pickupLng());
        ride.setDropAddress(req.dropAddress());
        ride.setDropLat(req.dropLat());
        ride.setDropLng(req.dropLng());
        ride.setDistanceKm(BigDecimal.valueOf(est.distanceKm()));
        ride.setDurationMin(est.durationMin());
        ride.setFare(est.fare());
        ride.setDiscount(est.discount());
        ride.setPromoCode(req.promoCode() == null || req.promoCode().isBlank() ? null : req.promoCode().toUpperCase());
        rides.save(ride);

        // Matching: nearest available drivers get a WebSocket push
        Ride detailed = rides.findDetailed(ride.getId()).orElseThrow();
        RideResponse response = RideResponse.of(detailed);
        List<Driver> nearby = drivers.findNearby(req.pickupLat(), req.pickupLng(), radiusKm, PageRequest.of(0, maxDrivers));
        nearby.forEach(d -> realtime.rideRequest(d.getUser().getId(), response));
        return response;
    }

    @Transactional
    public RideResponse cancel(AuthUser user, Long rideId) {
        Ride ride = load(rideId);
        boolean isRider = ride.getRider().getId().equals(user.id());
        boolean isDriver = ride.getDriver() != null && ride.getDriver().getUser().getId().equals(user.id());
        if (!isRider && !isDriver) throw ApiException.forbidden("Not your ride");
        ride.moveTo(RideStatus.CANCELLED);
        if (ride.getDriver() != null) ride.getDriver().setAvailable(true);
        realtime.rideStatus(rideId, "CANCELLED", "Ride cancelled");
        return RideResponse.of(ride);
    }

    // ---------- driver side ----------

    /** Open requests near the driver's current position. */
    @Transactional(readOnly = true)
    public List<RideResponse> openRequests(AuthUser user) {
        Driver d = driverService.byUser(user.id());
        if (d.getLatitude() == null) throw ApiException.badRequest("Send your location first");
        double dLat = radiusKm / 111.0;
        double dLng = radiusKm / (111.0 * Math.max(0.1, Math.cos(Math.toRadians(d.getLatitude()))));
        return rides.findOpenRequestsInBox(d.getLatitude() - dLat, d.getLatitude() + dLat,
                        d.getLongitude() - dLng, d.getLongitude() + dLng).stream()
                .filter(r -> GeoUtils.haversineKm(d.getLatitude(), d.getLongitude(), r.getPickupLat(), r.getPickupLng()) <= radiusKm)
                .map(RideResponse::of).toList();
    }

    @Transactional
    public RideResponse accept(AuthUser user, Long rideId) {
        Driver d = driverService.byUser(user.id());
        if (!d.isAvailable()) throw ApiException.conflict("Go online first (or you are already on a ride)");
        if (rides.findActiveByDriver(d.getId()).isPresent()) throw ApiException.conflict("Finish your current ride first");

        Ride ride = load(rideId);
        ride.moveTo(RideStatus.ACCEPTED);        // throws if someone else already took it
        ride.setDriver(d);
        d.setAvailable(false);
        rides.saveAndFlush(ride);                // @Version check happens here

        realtime.rideStatus(rideId, "ACCEPTED", d.getUser().getName() + " is on the way");
        drivers.findNearby(ride.getPickupLat(), ride.getPickupLng(), radiusKm, PageRequest.of(0, maxDrivers)).stream()
                .filter(o -> !o.getId().equals(d.getId()))
                .forEach(o -> realtime.rideRequestClosed(o.getUser().getId(), rideId));
        return RideResponse.of(ride);
    }

    /** Driver moves the ride forward: ARRIVED, STARTED, COMPLETED */
    @Transactional
    public RideResponse progress(AuthUser user, Long rideId, RideStatus next) {
        if (next != RideStatus.ARRIVED && next != RideStatus.STARTED && next != RideStatus.COMPLETED)
            throw ApiException.badRequest("Unsupported status");
        Ride ride = load(rideId);
        if (ride.getDriver() == null || !ride.getDriver().getUser().getId().equals(user.id()))
            throw ApiException.forbidden("Not your ride");
        ride.moveTo(next);
        if (next == RideStatus.COMPLETED) ride.getDriver().setAvailable(true);
        String msg = switch (next) {
            case ARRIVED -> "Your driver has arrived";
            case STARTED -> "Ride started";
            default -> "Ride completed. Please pay " + ride.getPayable();
        };
        realtime.rideStatus(rideId, next.name(), msg);
        return RideResponse.of(ride);
    }

    // ---------- common ----------

    @Transactional(readOnly = true)
    public RideResponse get(AuthUser user, Long rideId) {
        Ride r = load(rideId);
        assertCanView(user, r);
        return RideResponse.of(r);
    }

    @Transactional(readOnly = true)
    public Page<RideResponse> history(AuthUser user, Pageable p) {
        Page<Ride> page = (user.role() == Role.DRIVER)
                ? rides.findByDriver(driverService.byUser(user.id()).getId(), p)
                : rides.findByRider(user.id(), p);
        return page.map(RideResponse::of);
    }

    public Ride load(Long rideId) {
        return rides.findDetailed(rideId).orElseThrow(() -> ApiException.notFound("Ride not found"));
    }

    public void assertCanView(AuthUser user, Ride r) {
        boolean ok = user.role() == Role.ADMIN
                || r.getRider().getId().equals(user.id())
                || (r.getDriver() != null && r.getDriver().getUser().getId().equals(user.id()));
        if (!ok) throw ApiException.forbidden("Not your ride");
    }
}
