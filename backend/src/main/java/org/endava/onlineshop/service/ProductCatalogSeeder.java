package org.endava.onlineshop.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.endava.onlineshop.model.entities.Category;
import org.endava.onlineshop.model.entities.Product;
import org.endava.onlineshop.model.entities.ProductInventory;
import org.endava.onlineshop.repository.CategoryRepository;
import org.endava.onlineshop.repository.ProductRepository;
import org.springframework.core.io.Resource;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

// TODO: Remove before deploying to production.
@Component
public class ProductCatalogSeeder implements ApplicationRunner {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ObjectMapper objectMapper;
    private final Resource seedResource;

    public ProductCatalogSeeder(
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            ObjectMapper objectMapper,
            @org.springframework.beans.factory.annotation.Value("classpath:seed/mock-products.json") Resource seedResource
    ) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.objectMapper = objectMapper;
        this.seedResource = seedResource;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        loadSeedProducts().forEach(this::seedProduct);
    }

    private void seedProduct(SeedProduct seedProduct) {
        String slug = seedProduct.id();
        if (productRepository.existsBySlug(slug)) {
            return;
        }

        Category category = categoryRepository.findBySlug(toSlug(seedProduct.category()))
                .orElseGet(() -> createCategory(seedProduct.category()));

        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setSlug(slug);
        product.setName(seedProduct.title());
        product.setSku(toSku(slug));
        product.setBasePrice(BigDecimal.valueOf(seedProduct.price()));
        product.setDescription(seedProduct.description());
        product.setIsActive(true);
        product.setRating(seedProduct.rating());
        product.setReviewCount(seedProduct.reviewCount());
        product.setImagePlaceholder(seedProduct.imagePlaceholder());
        product.setImageGallery(new ArrayList<>(seedProduct.imageGallery()));
        product.setCategories(Set.of(category));

        ProductInventory inventory = new ProductInventory();
        inventory.setQuantityAvailable(25);
        inventory.setLowStockThreshold(5);
        product.setInventory(inventory);

        productRepository.save(product);
    }

    private Category createCategory(String categoryName) {
        Category category = new Category();
        category.setId(UUID.randomUUID());
        category.setName(categoryName);
        category.setSlug(toSlug(categoryName));
        return categoryRepository.save(category);
    }

    private List<SeedProduct> loadSeedProducts() {
        try {
            return objectMapper.readValue(seedResource.getInputStream(), new TypeReference<>() {});
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load product seed data", ex);
        }
    }

    private String toSlug(String value) {
        return value.toLowerCase().replace(" ", "-");
    }

    private String toSku(String slug) {
        return "SKU-" + slug.toUpperCase().replace("-", "_");
    }

    private record SeedProduct(
            String id,
            String category,
            String title,
            double rating,
            int reviewCount,
            double price,
            String description,
            String imagePlaceholder,
            List<String> imageGallery,
            List<SeedColorOption> availableColors
    ) {
    }

    private record SeedColorOption(String name, String swatch) {
    }
}




