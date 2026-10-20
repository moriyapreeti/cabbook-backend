package org.cabbook.promo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.cabbook.common.ApiException;
import org.springframework.stereotype.Service;

@Service
public class PromoService {
    private final PromoRepository repo;
    public PromoService(PromoRepository repo) { this.repo = repo; }

    public BigDecimal discountFor(String code, BigDecimal fare) {
        PromoCode p = repo.findByCodeIgnoreCase(code)
                .filter(PromoCode::isUsable)
                .orElseThrow(() -> ApiException.badRequest("Invalid or expired promo code"));
        BigDecimal d = fare.multiply(BigDecimal.valueOf(p.getDiscountPercent()))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        return d.min(p.getMaxDiscount());
    }
}
