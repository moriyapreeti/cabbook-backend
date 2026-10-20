package org.cabbook.driver;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public final class DriverDtos {
    private DriverDtos() {}

    public record LocationUpdate(@DecimalMin("-90") @DecimalMax("90") double latitude,
                                 @DecimalMin("-180") @DecimalMax("180") double longitude) {}

    public record AvailabilityUpdate(boolean available) {}

    public record DriverProfile(Long id, String name, String vehicleModel, String vehicleNumber,
                                boolean available, double avgRating, int ratingCount) {
        public static DriverProfile of(Driver d) {
            return new DriverProfile(d.getId(), d.getUser().getName(), d.getVehicleModel(),
                    d.getVehicleNumber(), d.isAvailable(), round(d.getAvgRating()), d.getRatingCount());
        }
        private static double round(double v) { return Math.round(v * 100.0) / 100.0; }
    }

    public record Earnings(BigDecimal total, long completedRides) {}
}
