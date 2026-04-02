package org.endava.onlineshop.model.mapper;

import org.endava.onlineshop.model.dto.CreateProductRequestDto;
import org.endava.onlineshop.model.dto.ProductResponseDto;
import org.endava.onlineshop.model.entities.Product;
import org.endava.onlineshop.model.entities.ProductImage;

public class ProductMapper {

    public Product toProductEntity(CreateProductRequestDto dto) {

        Product product = new Product();
        product.setName(dto.name());
        product.setDescription(dto.description());
        product.setPrice(dto.price());
        for(String imageUrl : dto.images()) {
            ProductImage productImage = new ProductImage();
            productImage.setUrl(imageUrl);
            productImage.setProduct(product);
            product.getImages().add(productImage);
        }

        return product;
    }

    public ProductResponseDto toProductDto(Product product) {

        return new ProductResponseDto(
            product.getId(),
            product.getName(),
            product.getDescription(),
            product.getPrice(),
            product.getImages().stream()
                .map(ProductImage::getUrl)
                .toList()
        );
    }
}
