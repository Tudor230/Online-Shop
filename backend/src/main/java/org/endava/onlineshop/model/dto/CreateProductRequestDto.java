package org.endava.onlineshop.model.dto;

import jakarta.validation.constraints.*;

import java.util.List;

public record CreateProductRequestDto(

    @NotBlank(message = "Product name must not be blank")
    @Max(value = 50, message = "Product name must not exceed 50 characters")
    String name,

    @NotBlank(message = "Product description must not be blank")
    @Max(value = 500, message = "Product description must not exceed 500 characters")
    String description,

    @NotNull(message = "Product price must not be null")
    @Min(value = 0, message = "Product price must be greater than or equal")
    Double price,

    @NotNull(message = "Product images must not be null")
    @NotEmpty(message = "At least one image is required")
    List<String> images
) {}
