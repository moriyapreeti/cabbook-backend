package org.cabbook.ride;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.cabbook.driver.DriverDtos.DriverProfile;

public final class RideDtos {
    private RideDtos() {}

    public record BookRideRequest(
            @NotBlank String pickupAddress,
            @DecimalMin("-90") @DecimalMax("90") double pickupLat,
            @DecimalMin("-180") @DecimalMax("180") double pickupLng,
            @NotBlank String dropAddress,
            @DecimalMin("-90") @DecimalMax("90") double dropLat,
            @DecimalMin("-180") @DecimalMax("180") double dropLng,
            String promoCode) {}

    public record FareEstimate(double distanceKm, int durationMin, BigDecimal fare, BigDecimal discount, BigDecimal payable) {}

    public record RideResponse(Long id, RideStatus status, String riderName,
                               String pickupAddress, double pickupLat, double pickupLng,
                               String dropAddress, double dropLat, double dropLng,
                               BigDecimal distanceKm, int durationMin,
                               BigDecimal fare, BigDecimal discount, BigDecimal payable,
                               DriverProfile driver, LocalDateTime requestedAt, LocalDateTime completedAt) {
        public static RideResponse of(Ride r) {
            return new RideResponse(r.getId(), r.getStatus(), r.getRider().getName(),
                    r.getPickupAddress(), r.getPickupLat(), r.getPickupLng(),
                    r.getDropAddress(), r.getDropLat(), r.getDropLng(),
                    r.getDistanceKm(), r.getDurationMin(), r.getFare(), r.getDiscount(), r.getPayable(),
                    r.getDriver() == null ? null : DriverProfile.of(r.getDriver()),
                    r.getRequestedAt(), r.getCompletedAt());
        }
    }
}
