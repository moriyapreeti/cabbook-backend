package org.cabbook.payment;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.stripe.net.Webhook;
import org.cabbook.auth.AuthUser;
import org.cabbook.common.ApiException;
import org.cabbook.payment.PaymentService.PaymentIntentResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    private final PaymentService service;
    private final String webhookSecret;

    public PaymentController(PaymentService service, @Value("${cabbook.stripe.webhook-secret}") String webhookSecret) {
        this.service = service; this.webhookSecret = webhookSecret;
    }

    @PostMapping("/ride/{rideId}/intent")
    @PreAuthorize("hasRole('RIDER')")
    public PaymentIntentResponse intent(@AuthenticationPrincipal AuthUser u, @PathVariable Long rideId) {
        return service.createIntent(u, rideId);
    }

    @PostMapping("/ride/{rideId}/mock-confirm")
    @PreAuthorize("hasRole('RIDER')")
    public void mockConfirm(@AuthenticationPrincipal AuthUser u, @PathVariable Long rideId) {
        service.mockConfirm(u, rideId);
    }

    /** Stripe calls this. Public URL, but protected by the Stripe signature check. */
    @PostMapping("/webhook")
    public ResponseEntity<String> webhook(@RequestBody String payload,
                                          @RequestHeader(value = "Stripe-Signature", required = false) String signature) {
        if (webhookSecret.isBlank()) throw ApiException.badRequest("Stripe webhook is not configured");
        Event event;
        try {
            event = Webhook.constructEvent(payload, signature, webhookSecret);
        } catch (SignatureVerificationException | NullPointerException e) {
            return ResponseEntity.badRequest().body("invalid signature");
        }

        EventDataObjectDeserializer d = event.getDataObjectDeserializer();
        StripeObject obj = d.getObject().orElseGet(() -> {
            try { return d.deserializeUnsafe(); } catch (Exception e) { return null; }
        });
        if (obj instanceof PaymentIntent pi) {
            switch (event.getType()) {
                case "payment_intent.succeeded" -> service.markPaid(pi.getId());
                case "payment_intent.payment_failed" -> service.markFailed(pi.getId());
                default -> {}
            }
        }
        return ResponseEntity.ok("ok");
    }
}
