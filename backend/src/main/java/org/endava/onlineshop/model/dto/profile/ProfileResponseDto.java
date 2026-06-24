package org.endava.onlineshop.model.dto.profile;

import java.util.List;
import java.util.UUID;

public record ProfileResponseDto(
        UUID id,
        String email,
        String firstName,
        String lastName,
        UUID defaultShippingAddressId,
        UUID defaultBillingAddressId,
        List<AddressResponseDto> addresses
) {
}
