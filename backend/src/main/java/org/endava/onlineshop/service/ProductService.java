package org.endava.onlineshop.service;

import org.endava.onlineshop.model.dto.product.ProductDetailsDto;
import org.endava.onlineshop.model.dto.product.ProductSummaryDto;
import org.endava.onlineshop.model.entities.Product;
import org.endava.onlineshop.repository.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<ProductSummaryDto> getActiveProducts() {
        return productRepository.findByIsActiveTrueOrderByNameAsc().stream()
                .map(this::toSummaryDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductDetailsDto getProductBySlug(String slug) {
        Product product = productRepository.findBySlugAndIsActiveTrue(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
        return toDetailsDto(product);
    }

    private ProductSummaryDto toSummaryDto(Product product) {
        return new ProductSummaryDto(
                product.getSlug(),
                extractPrimaryCategory(product),
                product.getName(),
                product.getRating(),
                product.getReviewCount(),
                product.getBasePrice(),
                product.getImagePlaceholder()
        );
    }

    private ProductDetailsDto toDetailsDto(Product product) {
        return new ProductDetailsDto(
                product.getSlug(),
                extractPrimaryCategory(product),
                product.getName(),
                product.getRating(),
                product.getReviewCount(),
                product.getBasePrice(),
                product.getDescription(),
                product.getImagePlaceholder(),
                List.copyOf(product.getImageGallery())
        );
    }

    private String extractPrimaryCategory(Product product) {
        return product.getCategories().stream()
                .map(category -> category.getName())
                .sorted()
                .findFirst()
                .orElse("Uncategorized");
    }
}

