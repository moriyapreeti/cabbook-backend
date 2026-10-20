package org.cabbook.payment;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

/** Active when STRIPE_SECRET_KEY is empty, so the whole project runs with zero paid accounts. */
@Component
@ConditionalOnExpression("'${cabbook.stripe.secret-key:}'.isEmpty()")
public class MockPaymentGateway implements PaymentGateway {
    @Override public String name() { return "MOCK"; }

    @Override
    public Intent createIntent(long amount, String currency, Long rideId) {
        return new Intent("mock_pi_" + rideId, "mock_secret_" + rideId);
    }
}
