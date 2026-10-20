package org.cabbook.payment;

/** Payment providers are pluggable. Stripe for production, Mock for local dev and tests. */
public interface PaymentGateway {
    record Intent(String providerRef, String clientSecret) {}

    String name();
    Intent createIntent(long amountMinorUnits, String currency, Long rideId);
}
