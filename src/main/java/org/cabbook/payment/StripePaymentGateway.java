package org.cabbook.payment;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.net.RequestOptions;
import com.stripe.param.PaymentIntentCreateParams;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnExpression("!'${cabbook.stripe.secret-key:}'.isEmpty()")
public class StripePaymentGateway implements PaymentGateway {
    @Value("${cabbook.stripe.secret-key}")
    private String secretKey;

    @PostConstruct
    void init() { Stripe.apiKey = secretKey; }

    @Override public String name() { return "STRIPE"; }

    @Override
    public Intent createIntent(long amount, String currency, Long rideId) {
        try {
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amount)
                    .setCurrency(currency)
                    .putMetadata("rideId", String.valueOf(rideId))
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder().setEnabled(true).build())
                    .build();
            // Same key for the same ride -> Stripe returns the same intent on retries (no double charge)
            RequestOptions opts = RequestOptions.builder().setIdempotencyKey("cabbook-ride-" + rideId).build();
            PaymentIntent pi = PaymentIntent.create(params, opts);
            return new Intent(pi.getId(), pi.getClientSecret());
        } catch (StripeException e) {
            throw new IllegalStateException("Stripe error: " + e.getMessage(), e);
        }
    }
}
