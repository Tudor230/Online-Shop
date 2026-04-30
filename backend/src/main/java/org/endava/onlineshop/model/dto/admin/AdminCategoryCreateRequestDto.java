package org.endava.onlineshop.model.dto.admin;

import java.util.UUID;

public record AdminCategoryCreateRequestDto(
        UUID parentId,
        String name,
        String slug
) {
}
