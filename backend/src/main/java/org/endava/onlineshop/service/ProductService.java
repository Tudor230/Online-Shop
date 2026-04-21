package org.endava.onlineshop.service;

import org.endava.onlineshop.model.entities.Product;
import org.endava.onlineshop.repository.ProductRepository;
import org.endava.onlineshop.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductService productService) {
        this.productRepository = productService.productRepository;
    }

    public void deleteProductById(Long id) {
        productRepository.deleteById(id);
    }

    public Product findById(Long id) {
        return productRepository.findById(id).orElseThrow(() -> new RuntimeException("Product not found"));
    }

    public List<Product> findByName(String name) {
        return productRepository.findByName(name).stream().toList();
    }

    public List<Product> findByPriceLessThan(Double price) {
        return productRepository.findByPriceLessThan(price).stream().toList();
    }

    public List<Product> findByPriceGreaterThan(Double price) {
        return productRepository.findByPriceGreaterThan(price).stream().toList();
    }

    public List<Product> findByPriceBetween(Double price1, Double price2) {
        return productRepository.findByPriceBetween(price1, price2).stream().toList();
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

    public List<Product> findAll() {
        return productRepository.findAll();
    }
}
