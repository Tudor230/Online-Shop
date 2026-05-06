package org.endava.onlineshop.service;

import lombok.RequiredArgsConstructor;
import org.endava.onlineshop.model.dto.product.ProductDetailsDto;
import org.endava.onlineshop.model.dto.product.ProductSearchPageDto;
import org.endava.onlineshop.model.dto.product.ProductSummaryDto;
import org.endava.onlineshop.model.entities.Category;
import org.endava.onlineshop.model.entities.Product;
import org.endava.onlineshop.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RequiredArgsConstructor
@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductEmbeddingService productEmbeddingService;

    @Transactional(readOnly = true)
    public ProductSearchPageDto getProducts(String query, Pageable pageable) {
        Page<Product> page = productEmbeddingService.findActiveProducts(query, pageable);

        List<ProductSummaryDto> items = page.getContent().stream()
            .map(this::toSummaryDto)
            .toList();

        return new ProductSearchPageDto(
            items,
            page.getNumber() + 1,
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.hasPrevious(),
            page.hasNext()
        );
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
                product.getImageId()
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
                product.getImageId(),
                List.copyOf(product.getImageGalleryIds())
        );
    }

    private String extractPrimaryCategory(Product product) {
        return product.getCategories().stream()
                .map(Category::getName)
                .sorted()
                .findFirst()
                .orElse("Uncategorized");
    }
}
