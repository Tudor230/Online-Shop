package org.endava.onlineshop.model.dto.admin;

public record AdminUploadedProductImageDto(
        String imageId,
        String imageUrl,
        String originalFilename
) {
}
