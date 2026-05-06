package org.endava.onlineshop.controller;

import org.endava.onlineshop.model.dto.product.ProductDetailsDto;
import org.endava.onlineshop.model.dto.product.ProductSearchPageDto;
import org.endava.onlineshop.service.ProductService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ProductSearchPageDto getProducts(
        @RequestParam(name = "q", required = false) String query,
        @PageableDefault(size = 25) Pageable pageable
    ) {
        return productService.getProducts(query, pageable);
    }

    @GetMapping("/{slug}")
    public ProductDetailsDto getProductBySlug(@PathVariable String slug) {
        return productService.getProductBySlug(slug);
    }
}
