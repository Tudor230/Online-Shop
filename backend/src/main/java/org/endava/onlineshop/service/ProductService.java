package org.endava.onlineshop.service;

import lombok.RequiredArgsConstructor;
import org.endava.onlineshop.model.dto.ProductResponseDto;
import org.endava.onlineshop.model.entities.Product;
import org.endava.onlineshop.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.endava.onlineshop.model.mapper.ProductMapper;

import java.util.List;

@RequiredArgsConstructor
@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper = new ProductMapper();


    public void deleteProductById(Long id) {
        productRepository.deleteById(id);
    }

    public Product findById(Long id) {
        return productRepository.findById(id).orElseThrow(() -> new RuntimeException("Product not found"));
    }

    public List<ProductResponseDto> findByName(String name) {
        return productRepository.findByName(name).stream().map(productMapper::toProductDto).toList();
    }

    public List<ProductResponseDto> findByPriceLessThan(Double price) {
        return productRepository.findByPriceLessThan(price).stream().map(productMapper::toProductDto).toList();
    }

    public List<ProductResponseDto> findByPriceGreaterThan(Double price) {
        return productRepository.findByPriceGreaterThan(price).stream().map(productMapper::toProductDto).toList();
    }

    public List<ProductResponseDto> findByPriceBetween(Double price1, Double price2) {
        return productRepository.findByPriceBetween(price1, price2).stream().map(productMapper::toProductDto).toList();
    }

    public Product createProduct(Product product) {
        if(product.getName() == null || product.getName().isBlank()) {
            throw new RuntimeException("Product name cannot be null or blank");
        }
        if(product.getDescription() == null || product.getDescription().isBlank()) {
            throw new RuntimeException("Product description cannot be null or blank");
        }
        if(product.getPrice() == null || product.getPrice() <= 0) {
            throw new RuntimeException("Product price cannot be null or less than zero");
        }
        return productRepository.save(product);
    }

    public List<ProductResponseDto> findAll() {
        return productRepository.findAll().stream().map(productMapper::toProductDto).toList();
    }
}
