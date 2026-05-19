package org.endava.onlineshop.model.dto.admin;

import jakarta.validation.constraints.NotNull;
import org.endava.onlineshop.model.enums.OrderStatus;

public record AdminOrderStatusUpdateDto(
        @NotNull OrderStatus newStatus,
        String notes
) {
}
