package org.endava.onlineshop.controller;

import lombok.RequiredArgsConstructor;
import org.endava.onlineshop.service.StripeCheckoutService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/checkout")
public class StripeWebhookController {

    private final StripeCheckoutService stripeCheckoutService;

    @PostMapping("/webhook")
    public ResponseEntity<Void> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signatureHeader
    ) {
        stripeCheckoutService.processWebhook(payload, signatureHeader);
        return ResponseEntity.ok().build();
    }
}
