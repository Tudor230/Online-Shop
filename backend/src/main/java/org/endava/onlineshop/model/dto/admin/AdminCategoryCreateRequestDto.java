package org.endava.onlineshop.model.dto.admin;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record AdminCategoryCreateRequestDto(
        UUID parentId,
        @NotNull @Size(max = 100) String name,
        @NotNull @Size(max = 120) String slug
) {
}
