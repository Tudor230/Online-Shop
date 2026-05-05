package org.endava.onlineshop.model.dto.cart;

import java.math.BigDecimal;

public record CartItemDto(
    String productId, String title, BigDecimal price, String imageId, Integer quantity) {}
