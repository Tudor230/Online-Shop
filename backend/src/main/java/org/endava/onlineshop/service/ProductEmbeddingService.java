package org.endava.onlineshop.service;

import org.endava.onlineshop.model.entities.Category;
import org.endava.onlineshop.model.entities.Product;
import org.endava.onlineshop.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class ProductEmbeddingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProductEmbeddingService.class);

    private static final String UPDATE_PRODUCT_EMBEDDING_SQL = """
            UPDATE product
            SET embedding = CAST(? AS vector),
                updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """;

    private final JdbcTemplate jdbcTemplate;
    private final ProductRepository productRepository;
    private final EmbeddingModel embeddingModel;
    private final boolean embeddingEnabled;
    private final double semanticMinSimilarity;
    private final int rrfK;
    private final double lexicalWeight;
    private final double semanticWeight;

    public ProductEmbeddingService(
            JdbcTemplate jdbcTemplate,
            ProductRepository productRepository,
            @Nullable EmbeddingModel embeddingModel,
            @Value("${ai.embedding.enabled:false}") boolean embeddingEnabled,
            @Value("${search.hybrid.semantic-min-similarity:0.58}") double semanticMinSimilarity,
            @Value("${search.hybrid.rrf.k:60}") int rrfK,
            @Value("${search.hybrid.weight.lexical:1.0}") double lexicalWeight,
            @Value("${search.hybrid.weight.semantic:1.0}") double semanticWeight
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.productRepository = productRepository;
        this.embeddingModel = embeddingModel;
        this.embeddingEnabled = embeddingEnabled;
        this.semanticMinSimilarity = semanticMinSimilarity;
        this.rrfK = rrfK;
        this.lexicalWeight = lexicalWeight;
        this.semanticWeight = semanticWeight;
    }

    public void upsertProductEmbedding(Product product) {
        if (!embeddingEnabled || product == null || product.getId() == null) {
            return;
        }

        String sourceText = buildProductEmbeddingText(product);
        String vector = createVectorLiteral(sourceText);
        if (vector == null) {
            return;
        }

        jdbcTemplate.update(UPDATE_PRODUCT_EMBEDDING_SQL, vector, product.getId());
    }

    public Page<Product> findActiveProducts(String query, Pageable pageable) {
        String normalizedQuery = normalizeQuery(query);
        boolean shouldUseSemantic = embeddingEnabled && !normalizedQuery.isBlank();
        String vectorLiteral = shouldUseSemantic ? createVectorLiteral(normalizedQuery) : null;
        boolean semanticEnabledForQuery = shouldUseSemantic && vectorLiteral != null;

        int limit = pageable.getPageSize();
        int offset = (int) pageable.getOffset();

        List<ProductRepository.ProductSearchResult> ranked = productRepository.search(
                normalizedQuery,
                semanticEnabledForQuery ? vectorLiteral : null,
                semanticEnabledForQuery,
                semanticMinSimilarity,
                rrfK,
                lexicalWeight,
                semanticWeight,
                limit,
                offset
        );

        List<UUID> rankedIds = ranked.stream()
                .map(ProductRepository.ProductSearchResult::getId)
                .toList();

        long totalItems = ranked.isEmpty()
                ? 0L
                : Optional.ofNullable(ranked.getFirst().getTotalCount()).orElse(0L);

        List<Product> orderedProducts = hydrateOrderedProducts(rankedIds);

        return new PageImpl<>(orderedProducts, pageable, totalItems);
    }

    private List<Product> hydrateOrderedProducts(List<UUID> rankedIds) {
        if (rankedIds.isEmpty()) {
            return List.of();
        }

        Map<UUID, Product> productById = productRepository.findByIdIn(rankedIds)
                .stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        return rankedIds.stream()
                .map(productById::get)
                .filter(product -> product != null && Boolean.TRUE.equals(product.getIsActive()))
                .toList();
    }

    private String normalizeQuery(String query) {
        return query == null ? "" : query.trim();
    }

    public String buildCategoryText(Set<Category> categories) {
        if (categories == null || categories.isEmpty()) {
            return "";
        }

        return categories.stream()
                .map(Category::getPath)
                .filter(path -> path != null && !path.isBlank())
                .map(path -> path.replace('.', ' ').replace('_', ' '))
                .flatMap(text -> Arrays.stream(text.split("\\s+")))
                .map(String::trim)
                .filter(word -> !word.isBlank())
                .distinct()
                .collect(Collectors.joining(" "));
    }

    private String buildProductEmbeddingText(Product product) {
        return String.join("\n",
                "name: " + product.getName(),
                "description: " + Optional.ofNullable(product.getDescription())
                        .orElse(""),
                "categories: " + Optional.ofNullable(product.getCategoryText())
                        .orElse("")
        );
    }

    private String createVectorLiteral(String text) {
        if (!embeddingEnabled || embeddingModel == null || text == null || text.isBlank()) {
            return null;
        }

        try {
            float[] vector = embeddingModel.embed(text);
            if (vector.length == 0) {
                return null;
            }

            return toVectorLiteral(vector);
        } catch (Exception exception) {
            LOGGER.warn("Embedding generation failed, semantic branch will be skipped", exception);
            return null;
        }
    }

    private String toVectorLiteral(float[] vector) {
        return IntStream.range(0, vector.length)
                .mapToObj(index -> String.format(java.util.Locale.US, "%.8f", vector[index]))
                .collect(Collectors.joining(",", "[", "]"));
    }
}

