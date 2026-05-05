package org.endava.onlineshop.controller;

import java.util.List;
import org.endava.onlineshop.model.dto.product.ProductDetailsDto;
import org.endava.onlineshop.model.dto.product.ProductSummaryDto;
import org.endava.onlineshop.service.ProductService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
public class ProductController {

  private final ProductService productService;

  public ProductController(ProductService productService) {
    this.productService = productService;
  }

  @GetMapping
  public List<ProductSummaryDto> getProducts() {
    return productService.getActiveProducts();
  }

  @GetMapping("/{slug}")
  public ProductDetailsDto getProductBySlug(@PathVariable String slug) {
    return productService.getProductBySlug(slug);
  }
}
