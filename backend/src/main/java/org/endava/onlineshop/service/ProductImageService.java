package org.endava.onlineshop.service;

import org.endava.onlineshop.model.entities.ProductImage;
import org.endava.onlineshop.repository.ProductImageRepository;
import org.endava.onlineshop.repository.ProductRepository;
import org.springframework.stereotype.Service;

@Service
public class ProductImageService {
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;

    public ProductImageService(ProductRepository productRepository, ProductImageRepository productImageRepository) {
        this.productRepository = productRepository;
        this.productImageRepository = productImageRepository;
    }

    public void createProductImage(String url, Long productId) {
        ProductImage productImage = new ProductImage();
        productImage.setUrl(url);
        productImage.setProduct(productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId)));
        productImageRepository.save(productImage);
    }

    public ProductImage getProductImageById(Long id) {
        return productImageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product image not found with id: " + id));
    }

    public ProductImage getProductImageByProductId(Long productId) {
        return productImageRepository.findByProductId(productId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Product image not found for product id: " + productId));
    }

    public void deleteProductImage(Long id) {
        productImageRepository.deleteById(id);
    }
}
