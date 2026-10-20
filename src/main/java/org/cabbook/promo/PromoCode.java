package org.cabbook.promo;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "promo_codes")
@Getter @Setter @NoArgsConstructor
public class PromoCode {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String code;
    private int discountPercent;
    private BigDecimal maxDiscount;
    private boolean active;
    private LocalDateTime expiresAt;

    public boolean isUsable() { return active && (expiresAt == null || expiresAt.isAfter(LocalDateTime.now())); }
}
