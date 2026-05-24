package org.endava.onlineshop.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.endava.onlineshop.model.dto.product.CreateProductReviewRequestDto;
import org.endava.onlineshop.model.dto.product.ProductDetailsDto;
import org.endava.onlineshop.model.dto.product.ProductSearchPageDto;
import org.endava.onlineshop.model.entities.User;
import org.endava.onlineshop.service.ProductService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ProductSearchPageDto getProducts(
        @RequestParam(name = "q", required = false) String query,
        @PageableDefault(size = 25) Pageable pageable
    ) {
        return productService.getProducts(query, pageable);
    }

    @GetMapping("/{slug}")
    public ProductDetailsDto getProductBySlug(
            @PathVariable String slug,
            @AuthenticationPrincipal User user
    ) {
        return productService.getProductBySlug(slug, user);
    }

    @PostMapping("/{productId}/reviews")
    @PreAuthorize("isAuthenticated()")
    public ProductDetailsDto createReview(
            @PathVariable UUID productId,
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreateProductReviewRequestDto request
    ) {
        return productService.createReview(productId, user, request);
    }
}
