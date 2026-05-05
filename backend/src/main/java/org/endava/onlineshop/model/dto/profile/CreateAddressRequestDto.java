package org.endava.onlineshop.model.dto.profile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateAddressRequestDto(
    @NotBlank(message = "Recipient name must not be blank")
        @Size(max = 200, message = "Recipient name must not exceed 200 characters")
        String recipientName,
    @Size(max = 20, message = "Phone number must not exceed 20 characters") String phoneNumber,
    @NotBlank(message = "Address line 1 must not be blank")
        @Size(max = 255, message = "Address line 1 must not exceed 255 characters")
        String addressLine1,
    @Size(max = 255, message = "Address line 2 must not exceed 255 characters") String addressLine2,
    @NotBlank(message = "City must not be blank")
        @Size(max = 100, message = "City must not exceed 100 characters")
        String city,
    @NotBlank(message = "State must not be blank")
        @Size(max = 100, message = "State must not exceed 100 characters")
        String state,
    @NotBlank(message = "Postal code must not be blank")
        @Size(max = 20, message = "Postal code must not exceed 20 characters")
        String postalCode,
    @NotBlank(message = "Country must not be blank")
        @Size(max = 100, message = "Country must not exceed 100 characters")
        String country) {}
