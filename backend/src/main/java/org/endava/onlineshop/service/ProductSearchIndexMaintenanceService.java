package org.endava.onlineshop.service;

import org.endava.onlineshop.model.entities.Product;
import org.endava.onlineshop.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ProductSearchIndexMaintenanceService {

    private final ProductRepository productRepository;
    private final ProductEmbeddingService productEmbeddingService;
    private final int categoryReindexBatchSize;

    public ProductSearchIndexMaintenanceService(
            ProductRepository productRepository,
            ProductEmbeddingService productEmbeddingService,
            @Value("${search.reindex.category-batch-size:100}") int categoryReindexBatchSize
    ) {
        this.productRepository = productRepository;
        this.productEmbeddingService = productEmbeddingService;
        this.categoryReindexBatchSize = categoryReindexBatchSize;
    }

    @Transactional
    public void reindexProduct(UUID productId) {
        productRepository.findWithCategoriesById(productId)
                .ifPresent(this::updateCategoryTextAndEmbedding);
    }

    @Transactional
    public void reindexProductsForCategory(UUID categoryId) {
        int page = 0;

        while (true) {
            Page<UUID> productIds = productRepository.findProductIdsByCategoryId(
                    categoryId,
                    PageRequest.of(page, categoryReindexBatchSize)
            );

            if (productIds.isEmpty()) {
                return;
            }

            List<Product> products = productRepository.findByIdIn(productIds.getContent());
            for (Product product : products) {
                updateCategoryTextAndEmbedding(product);
            }

            if (!productIds.hasNext()) {
                return;
            }
            page++;
        }
    }

    private void updateCategoryTextAndEmbedding(Product product) {
        String categoryText = productEmbeddingService.buildCategoryText(product.getCategories());
        product.setCategoryText(categoryText);
        Product savedProduct = productRepository.saveAndFlush(product);
        productEmbeddingService.upsertProductEmbedding(savedProduct);
    }
}

