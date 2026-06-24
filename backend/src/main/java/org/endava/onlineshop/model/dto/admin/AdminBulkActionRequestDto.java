package org.endava.onlineshop.model.dto.admin;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record AdminBulkActionRequestDto(
        @NotEmpty List<UUID> ids
) {
}
