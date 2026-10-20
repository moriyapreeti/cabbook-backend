package org.cabbook.driver;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.cabbook.common.ApiException;
import org.cabbook.driver.DriverDtos.*;
import org.cabbook.realtime.RealtimePublisher;
import org.cabbook.ride.Ride;
import org.cabbook.ride.RideRepository;
import org.cabbook.ride.RideStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DriverService {
    private final DriverRepository drivers;
    private final RideRepository rides;
    private final RealtimePublisher realtime;

    public DriverService(DriverRepository drivers, RideRepository rides, RealtimePublisher realtime) {
        this.drivers = drivers;
        this.rides = rides;
        this.realtime = realtime;
    }

    public Driver byUser(Long userId) {
        return drivers.findByUserId(userId).orElseThrow(() -> ApiException.notFound("Driver profile not found"));
    }

    @Transactional(readOnly = true)
    public DriverProfile profile(Long userId) {
        return DriverProfile.of(byUser(userId));
    }

    @Transactional
    public DriverProfile setAvailability(Long userId, boolean available) {
        Driver d = byUser(userId);
        d.setAvailable(available);
        return DriverProfile.of(d);
    }

    @Transactional
    public void updateLocation(Long userId, LocationUpdate loc) {
        Driver d = byUser(userId);
        d.setLatitude(loc.latitude());
        d.setLongitude(loc.longitude());
        d.setLocationUpdatedAt(LocalDateTime.now());
        // If the driver is on an active ride, stream the location to the rider
        rides.findActiveByDriver(d.getId())
                .ifPresent(r -> realtime.driverLocation(r.getId(), loc.latitude(), loc.longitude()));
    }

    @Transactional(readOnly = true)
    public Earnings earnings(Long userId) {
        Driver d = byUser(userId);
        BigDecimal total = rides.sumFareByDriverAndStatus(d.getId(), RideStatus.COMPLETED);
        if (total == null)
            total = BigDecimal.ZERO;
        return new Earnings(total, rides.countByDriverIdAndStatus(d.getId(), RideStatus.COMPLETED));
    }

    @Transactional(readOnly = true)
    public List<DriverProfile> top(Pageable p) {
        return drivers.findTopRated(p).stream().map(DriverProfile::of).toList();
    }
}
