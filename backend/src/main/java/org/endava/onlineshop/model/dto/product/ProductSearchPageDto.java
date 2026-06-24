package org.endava.onlineshop.model.dto.product;

import java.util.List;

public record ProductSearchPageDto(
        List<ProductSummaryDto> items,
        int page,
        int size,
        long totalItems,
        int totalPages,
        boolean hasPrevious,
        boolean hasNext
) {
}
