package org.endava.onlineshop.model.dto.category;

import java.util.List;
import java.util.UUID;

public record CategoryTreeNodeDto(
        UUID id,
        UUID parentId,
        String name,
        String slug,
        String path,
        List<CategoryTreeNodeDto> children
) {}
