package org.cabbook.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.cabbook.auth.AuthUser;
import org.cabbook.common.ApiException;
import org.cabbook.receipt.ReceiptService;
import org.cabbook.ride.Ride;
import org.cabbook.ride.RideRepository;
import org.cabbook.ride.RideStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {
    public record PaymentIntentResponse(Long rideId, BigDecimal amount, String currency, String provider, String clientSecret) {}

    private final PaymentGateway gateway;
    private final PaymentRepository payments;
    private final RideRepository rides;
    private final ReceiptService receipts;
    private final String currency;

    public PaymentService(PaymentGateway gateway, PaymentRepository payments, RideRepository rides,
                          ReceiptService receipts, @Value("${cabbook.stripe.currency}") String currency) {
        this.gateway = gateway; this.payments = payments; this.rides = rides; this.receipts = receipts; this.currency = currency;
    }

    /** Called by the rider's app after the ride is COMPLETED. The returned clientSecret goes to Stripe.js on the frontend. */
    @Transactional
    public PaymentIntentResponse createIntent(AuthUser rider, Long rideId) {
        Ride ride = rides.findDetailed(rideId).orElseThrow(() -> ApiException.notFound("Ride not found"));
        if (!ride.getRider().getId().equals(rider.id())) throw ApiException.forbidden("Not your ride");
        if (ride.getStatus() != RideStatus.COMPLETED) throw ApiException.conflict("Ride is not completed yet");

        Payment p = payments.findByRideId(rideId).orElseGet(() -> {
            Payment n = new Payment();
            n.setRide(ride);
            return n;
        });
        if (p.getStatus() == PaymentStatus.PAID) throw ApiException.conflict("Already paid");

        BigDecimal amount = ride.getPayable();
        long minor = amount.movePointRight(2).longValueExact();
        PaymentGateway.Intent intent = gateway.createIntent(minor, currency, rideId);

        p.setAmount(amount);
        p.setCurrency(currency);
        p.setProvider(gateway.name());
        p.setProviderRef(intent.providerRef());
        p.setStatus(PaymentStatus.PENDING);
        payments.save(p);
        return new PaymentIntentResponse(rideId, amount, currency, gateway.name(), intent.clientSecret());
    }

    /** Idempotent: Stripe may deliver the same webhook several times. */
    @Transactional
    public void markPaid(String providerRef) {
        Payment p = payments.findByProviderRef(providerRef).orElse(null);
        if (p == null || p.getStatus() == PaymentStatus.PAID) return;
        p.setStatus(PaymentStatus.PAID);
        p.setPaidAt(LocalDateTime.now());
        Long rideId = p.getRide().getId();
        receipts.emailReceiptAsync(rideId);     // @Async, will not block or break the webhook
    }

    @Transactional
    public void markFailed(String providerRef) {
        payments.findByProviderRef(providerRef).ifPresent(p -> {
            if (p.getStatus() == PaymentStatus.PENDING) p.setStatus(PaymentStatus.FAILED);
        });
    }

    /** Dev-only: pretend the card payment succeeded (only works with the mock gateway). */
    @Transactional
    public void mockConfirm(AuthUser rider, Long rideId) {
        if (!"MOCK".equals(gateway.name())) throw ApiException.forbidden("Only available with the mock gateway");
        Payment p = payments.findByRideId(rideId).orElseThrow(() -> ApiException.notFound("Create the payment intent first"));
        if (!p.getRide().getRider().getId().equals(rider.id())) throw ApiException.forbidden("Not your ride");
        markPaid(p.getProviderRef());
    }
}
