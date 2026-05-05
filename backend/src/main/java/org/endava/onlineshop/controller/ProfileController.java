package org.endava.onlineshop.controller;

import jakarta.validation.Valid;
import java.util.UUID;
import org.endava.onlineshop.model.dto.profile.CreateAddressRequestDto;
import org.endava.onlineshop.model.dto.profile.ProfileResponseDto;
import org.endava.onlineshop.model.dto.profile.SetPrimaryAddressRequestDto;
import org.endava.onlineshop.model.dto.profile.UpdateProfileRequestDto;
import org.endava.onlineshop.model.entities.User;
import org.endava.onlineshop.service.ProfileService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

  private final ProfileService profileService;

  public ProfileController(ProfileService profileService) {
    this.profileService = profileService;
  }

  @GetMapping
  public ProfileResponseDto getProfile(@AuthenticationPrincipal User user) {
    return profileService.getProfile(user);
  }

  @PatchMapping
  public ProfileResponseDto updateProfile(
      @AuthenticationPrincipal User user, @Valid @RequestBody UpdateProfileRequestDto request) {
    return profileService.updateProfile(user, request);
  }

  @PostMapping("/addresses")
  public ProfileResponseDto createAddress(
      @AuthenticationPrincipal User user, @Valid @RequestBody CreateAddressRequestDto request) {
    return profileService.createAddress(user, request);
  }

  @PatchMapping("/primary-shipping")
  public ProfileResponseDto setPrimaryShippingAddress(
      @AuthenticationPrincipal User user, @Valid @RequestBody SetPrimaryAddressRequestDto request) {
    return profileService.setPrimaryShippingAddress(user, request.addressId());
  }

  @PatchMapping("/primary-billing")
  public ProfileResponseDto setPrimaryBillingAddress(
      @AuthenticationPrincipal User user, @Valid @RequestBody SetPrimaryAddressRequestDto request) {
    return profileService.setPrimaryBillingAddress(user, request.addressId());
  }

  @DeleteMapping("/addresses/{addressId}")
  public ProfileResponseDto deleteAddress(
      @AuthenticationPrincipal User user, @PathVariable UUID addressId) {
    return profileService.deleteAddress(user, addressId);
  }
}
