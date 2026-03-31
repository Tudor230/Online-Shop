package org.endava.onlineshop.model.dto;

import java.util.List;

public record ProductResponseDto(
    Long id,
    String name,
    String description,
    Double price,
    List<String> images) {
}
