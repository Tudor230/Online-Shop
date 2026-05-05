package org.endava.onlineshop.model.dto.profile;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record SetPrimaryAddressRequestDto(
    @NotNull(message = "Address id must not be null") UUID addressId) {}
