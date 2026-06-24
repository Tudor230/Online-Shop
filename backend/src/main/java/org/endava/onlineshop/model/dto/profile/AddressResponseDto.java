package org.endava.onlineshop.model.dto.profile;

import java.time.Instant;
import java.util.UUID;

public record AddressResponseDto(
        UUID id,
        String recipientName,
        String phoneNumber,
        String addressLine1,
        String addressLine2,
        String city,
        String state,
        String postalCode,
        String country,
        Instant createdAt,
        Instant updatedAt
) {
}
