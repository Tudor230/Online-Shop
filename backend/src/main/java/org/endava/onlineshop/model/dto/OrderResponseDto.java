package org.endava.onlineshop.model.dto;

import org.endava.onlineshop.model.enums.OrderStatus;

import java.util.List;

public record OrderResponseDto(
        Long id,
        UserResponseDto user,
        List<ProductResponseDto> products,
        OrderStatus status) {
}
