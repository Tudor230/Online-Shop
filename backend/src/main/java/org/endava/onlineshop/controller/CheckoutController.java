package org.endava.onlineshop.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.endava.onlineshop.model.dto.checkout.CheckoutStatusResponseDto;
import org.endava.onlineshop.model.dto.checkout.CreateCheckoutSessionRequestDto;
import org.endava.onlineshop.model.dto.checkout.CreateCheckoutSessionResponseDto;
import org.endava.onlineshop.model.entities.User;
import org.endava.onlineshop.service.StripeCheckoutService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/checkout")
public class CheckoutController {

    private final StripeCheckoutService stripeCheckoutService;

    @PostMapping("/session")
    public CreateCheckoutSessionResponseDto createCheckoutSession(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreateCheckoutSessionRequestDto request
    ) {
        return stripeCheckoutService.createCheckoutSession(user, request);
    }

    @PostMapping("/session/order/{orderId}")
    public CreateCheckoutSessionResponseDto createCheckoutSessionForOrder(
            @AuthenticationPrincipal User user,
            @PathVariable UUID orderId,
            @Valid @RequestBody CreateCheckoutSessionRequestDto request
    ) {
        return stripeCheckoutService.createCheckoutSessionForOrder(user, orderId, request);
    }

    @GetMapping("/status/{orderId}")
    public CheckoutStatusResponseDto getCheckoutStatus(
            @AuthenticationPrincipal User user,
            @PathVariable UUID orderId
    ) {
        return stripeCheckoutService.getCheckoutStatus(user, orderId);
    }
}
