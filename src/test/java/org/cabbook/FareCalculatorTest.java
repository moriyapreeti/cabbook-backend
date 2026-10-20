package org.cabbook;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import org.cabbook.pricing.FareCalculator;
import org.cabbook.pricing.GeoUtils;
import org.junit.jupiter.api.Test;

class FareCalculatorTest {
    private final FareCalculator calc = new FareCalculator(
            new BigDecimal("40"), new BigDecimal("12"), new BigDecimal("1.5"), new BigDecimal("60"));

    @Test
    void normalFare() {
        // 40 + 12*10 + 1.5*20 = 190
        assertEquals(new BigDecimal("190.00"), calc.calculate(10, 20));
    }

    @Test
    void minimumFareApplies() {
        // 40 + 12*0.5 + 1.5*2 = 49 -> below minimum 60
        assertEquals(new BigDecimal("60"), calc.calculate(0.5, 2));
    }

    @Test
    void haversineLudhianaToChandigarh() {
        double km = GeoUtils.haversineKm(30.9010, 75.8573, 30.7333, 76.7794);
        assertTrue(km > 85 && km < 95, "expected ~90 km but was " + km);
    }
}
