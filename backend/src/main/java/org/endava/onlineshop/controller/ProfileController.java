package org.endava.onlineshop.controller;

import jakarta.validation.Valid;
import org.endava.onlineshop.model.dto.profile.CreateAddressRequestDto;
import org.endava.onlineshop.model.dto.profile.ProfileResponseDto;
import org.endava.onlineshop.model.dto.profile.SetPrimaryAddressRequestDto;
import org.endava.onlineshop.model.dto.profile.UpdateProfileRequestDto;
import org.endava.onlineshop.service.ProfileService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public ProfileResponseDto getProfile(@AuthenticationPrincipal Jwt jwt) {
        return profileService.getProfile(jwt);
    }

    @PatchMapping
    public ProfileResponseDto updateProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateProfileRequestDto request
    ) {
        return profileService.updateProfile(jwt, request);
    }

    @PostMapping("/addresses")
    public ProfileResponseDto createAddress(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateAddressRequestDto request
    ) {
        return profileService.createAddress(jwt, request);
    }

    @PatchMapping("/primary-shipping")
    public ProfileResponseDto setPrimaryShippingAddress(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody SetPrimaryAddressRequestDto request
    ) {
        return profileService.setPrimaryShippingAddress(jwt, request.addressId());
    }

    @PatchMapping("/primary-billing")
    public ProfileResponseDto setPrimaryBillingAddress(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody SetPrimaryAddressRequestDto request
    ) {
        return profileService.setPrimaryBillingAddress(jwt, request.addressId());
    }

    @DeleteMapping("/addresses/{addressId}")
    public ProfileResponseDto deleteAddress(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID addressId
    ) {
        return profileService.deleteAddress(jwt, addressId);
    }
}
