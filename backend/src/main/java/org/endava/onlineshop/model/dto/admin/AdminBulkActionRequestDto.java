package org.endava.onlineshop.model.dto.admin;

import java.util.List;
import java.util.UUID;

public record AdminBulkActionRequestDto(
        List<UUID> ids
) {
}
