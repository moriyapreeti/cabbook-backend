package org.cabbook.rating;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import org.cabbook.auth.AuthUser;
import org.cabbook.common.ApiException;
import org.cabbook.driver.DriverRepository;
import org.cabbook.ride.Ride;
import org.cabbook.ride.RideRepository;
import org.cabbook.ride.RideStatus;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RatingService {
    public record RatingRequest(@Min(1) @Max(5) int stars, @Size(max = 500) String comment) {}
    public record RatingResponse(Long rideId, int stars, String comment, LocalDateTime createdAt) {}

    private final RatingRepository ratings;
    private final RideRepository rides;
    private final DriverRepository drivers;

    public RatingService(RatingRepository ratings, RideRepository rides, DriverRepository drivers) {
        this.ratings = ratings; this.rides = rides; this.drivers = drivers;
    }

    @Transactional
    public RatingResponse rate(AuthUser rider, Long rideId, RatingRequest req) {
        Ride ride = rides.findDetailed(rideId).orElseThrow(() -> ApiException.notFound("Ride not found"));
        if (!ride.getRider().getId().equals(rider.id())) throw ApiException.forbidden("Not your ride");
        if (ride.getStatus() != RideStatus.COMPLETED) throw ApiException.conflict("You can rate only completed rides");
        if (ratings.existsByRideId(rideId)) throw ApiException.conflict("Ride already rated");

        Rating r = new Rating();
        r.setRide(ride);
        r.setDriver(ride.getDriver());
        r.setStars(req.stars());
        r.setComment(req.comment());
        try {
            ratings.saveAndFlush(r);            // UNIQUE(ride_id) also protects against double submits
        } catch (DataIntegrityViolationException e) {
            throw ApiException.conflict("Ride already rated");
        }
        drivers.addRating(ride.getDriver().getId(), req.stars());
        return new RatingResponse(rideId, r.getStars(), r.getComment(), r.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public Page<RatingResponse> forDriver(Long driverId, Pageable p) {
        return ratings.findByDriverIdOrderByCreatedAtDesc(driverId, p)
                .map(r -> new RatingResponse(r.getRide().getId(), r.getStars(), r.getComment(), r.getCreatedAt()));
    }
}
