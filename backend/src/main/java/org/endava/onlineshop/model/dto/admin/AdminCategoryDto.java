package org.endava.onlineshop.model.dto.admin;

import java.util.UUID;

public record AdminCategoryDto(
        UUID id,
        UUID parentId,
        String name,
        String slug
) {
}
