package org.endava.onlineshop.repository;

import org.endava.onlineshop.model.entities.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    interface ProductSearchResult {
        UUID getId();

        Long getTotalCount();
    }

    @EntityGraph(attributePaths = {"categories", "inventory"})
    Optional<Product> findBySlugAndIsActiveTrue(String slug);

    @EntityGraph(attributePaths = {"categories", "inventory"})
    Optional<Product> findBySlug(String slug);

    @EntityGraph(attributePaths = {"categories", "inventory"})
    List<Product> findByIdIn(Collection<UUID> ids);

    @EntityGraph(attributePaths = {"categories", "inventory"})
    Optional<Product> findWithCategoriesById(UUID id);

    boolean existsBySlug(String slug);

    @Query("""
          SELECT p.id
          FROM Product p
          JOIN p.categories c
          WHERE c.id = :categoryId
          """)
    Page<UUID> findProductIdsByCategoryId(@Param("categoryId") UUID categoryId, Pageable pageable);

    @Query(value = """
            SELECT p.*
            FROM product p
            JOIN product seed ON seed.id = :productId
            WHERE p.is_active = TRUE
              AND p.id <> seed.id
              AND p.embedding IS NOT NULL
              AND seed.embedding IS NOT NULL
              AND (1 - (p.embedding <=> seed.embedding)) >= :minSimilarity
            ORDER BY p.embedding <=> seed.embedding ASC, p.name ASC
            LIMIT :limit
            """, nativeQuery = true)
    List<Product> findSimilarProductsByEmbedding(
            @Param("productId") UUID productId,
            @Param("minSimilarity") double minSimilarity,
            @Param("limit") int limit
    );

    @Query(value = """
            WITH lexical AS (
                SELECT p.id,
                       ROW_NUMBER() OVER (
                            ORDER BY ts_rank_cd(
                                (
                                     setweight(to_tsvector('english', p.name), 'A') ||
                                     setweight(to_tsvector('english', COALESCE(p.category_text, '')), 'B') ||
                                     setweight(to_tsvector('english', COALESCE(p.description, '')), 'C')
                                ),
                                websearch_to_tsquery('english', :query)
                            ) DESC,
                            p.name ASC
                        ) AS rank_position
                FROM product p
                WHERE p.is_active = TRUE
                  AND (:categoryPath IS NULL OR EXISTS (
                      SELECT 1 FROM product_category pc
                      JOIN category c ON pc.category_id = c.id
                      WHERE pc.product_id = p.id AND c.path <@ CAST(:categoryPath AS ltree)
                  ))
                  AND (
                      :query = ''
                      OR (
                          setweight(to_tsvector('english', p.name), 'A') ||
                          setweight(to_tsvector('english', COALESCE(p.category_text, '')), 'B') ||
                          setweight(to_tsvector('english', COALESCE(p.description, '')), 'C')
                      ) @@ websearch_to_tsquery('english', :query)
                  )
            ),
            semantic AS (
                SELECT p.id,
                       ROW_NUMBER() OVER (
                            ORDER BY p.embedding <=> CAST(:queryEmbedding AS vector) ASC,
                            p.name ASC
                        ) AS rank_position
                FROM product p
                WHERE p.is_active = TRUE
                  AND :useSemantic = TRUE
                  AND p.embedding IS NOT NULL
                  AND (:categoryPath IS NULL OR EXISTS (
                      SELECT 1 FROM product_category pc
                      JOIN category c ON pc.category_id = c.id
                      WHERE pc.product_id = p.id AND c.path <@ CAST(:categoryPath AS ltree)
                  ))
                  AND (1 - (p.embedding <=> CAST(:queryEmbedding AS vector))) >= :semanticMinSimilarity
            ),
            fused AS (
                SELECT id,
                       SUM(score) AS rrf_score
                FROM (
                    SELECT id, CAST(:lexicalWeight AS DOUBLE PRECISION) / (:rrfK + rank_position) AS score
                    FROM lexical
                    UNION ALL
                    SELECT id, CAST(:semanticWeight AS DOUBLE PRECISION) / (:rrfK + rank_position) AS score
                    FROM semantic
                ) ranked
                GROUP BY id
            )
            SELECT f.id AS id,
                   COUNT(*) OVER () AS totalCount
            FROM fused f
            ORDER BY f.rrf_score DESC, f.id
            LIMIT :limit OFFSET :offset
            """, nativeQuery = true)
    List<ProductSearchResult> search(
            @Param("query") String query,
            @Param("queryEmbedding") String queryEmbedding,
            @Param("useSemantic") boolean useSemantic,
            @Param("semanticMinSimilarity") double semanticMinSimilarity,
            @Param("rrfK") int rrfK,
            @Param("lexicalWeight") double lexicalWeight,
            @Param("semanticWeight") double semanticWeight,
            @Param("categoryPath") String categoryPath,
            @Param("limit") int limit,
            @Param("offset") int offset
    );
}
