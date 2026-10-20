package org.cabbook.pricing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** fare = max(minimum, base + perKm * km + perMinute * minutes) */
@Component
public class FareCalculator {
    private final BigDecimal base, perKm, perMinute, minimum;

    public FareCalculator(@Value("${cabbook.fare.base}") BigDecimal base,
                          @Value("${cabbook.fare.per-km}") BigDecimal perKm,
                          @Value("${cabbook.fare.per-minute}") BigDecimal perMinute,
                          @Value("${cabbook.fare.minimum}") BigDecimal minimum) {
        this.base = base; this.perKm = perKm; this.perMinute = perMinute; this.minimum = minimum;
    }

    public BigDecimal calculate(double distanceKm, int durationMin) {
        BigDecimal fare = base
                .add(perKm.multiply(BigDecimal.valueOf(distanceKm)))
                .add(perMinute.multiply(BigDecimal.valueOf(durationMin)))
                .setScale(2, RoundingMode.HALF_UP);
        return fare.max(minimum);
    }
}
