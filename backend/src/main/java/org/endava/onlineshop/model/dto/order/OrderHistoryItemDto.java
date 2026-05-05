package org.endava.onlineshop.model.dto.order;

import java.math.BigDecimal;

public record OrderHistoryItemDto(
    String productSlug,
    String title,
    String imageId,
    int quantity,
    BigDecimal unitPrice,
    BigDecimal lineTotal) {}
