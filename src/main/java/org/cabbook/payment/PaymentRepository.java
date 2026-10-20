package org.cabbook.payment;

import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByRideId(Long rideId);
    Optional<Payment> findByProviderRef(String providerRef);

    @Query("select sum(p.amount) from Payment p where p.status = org.cabbook.payment.PaymentStatus.PAID")
    BigDecimal totalPaid();
}
