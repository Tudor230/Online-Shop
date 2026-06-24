package org.endava.onlineshop.service;

import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.endava.onlineshop.exception.BadRequestException;
import org.endava.onlineshop.model.dto.product.CreateProductReviewRequestDto;
import org.endava.onlineshop.model.dto.product.ProductDetailsDto;
import org.endava.onlineshop.model.dto.product.ProductReviewDto;
import org.endava.onlineshop.model.dto.product.ProductSearchPageDto;
import org.endava.onlineshop.model.dto.product.ProductSummaryDto;
import org.endava.onlineshop.model.entities.Category;
import org.endava.onlineshop.model.entities.Product;
import org.endava.onlineshop.model.entities.Review;
import org.endava.onlineshop.model.entities.User;
import org.endava.onlineshop.model.enums.OrderStatus;
import org.endava.onlineshop.repository.OrderRepository;
import org.endava.onlineshop.repository.ProductRepository;
import org.endava.onlineshop.repository.ReviewRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class ProductService {

    private static final EnumSet<OrderStatus> REVIEW_ELIGIBLE_ORDER_STATUSES = EnumSet.of(
            OrderStatus.PAID,
            OrderStatus.PROCESSING,
            OrderStatus.SHIPPED,
            OrderStatus.DELIVERED,
            OrderStatus.RETURNED
    );

    private final ProductRepository productRepository;
    private final ReviewRepository reviewRepository;
    private final OrderRepository orderRepository;
    private final ProductEmbeddingService productEmbeddingService;

    @Transactional(readOnly = true)
    public ProductSearchPageDto getProducts(String query, String categorySlug, Pageable pageable) {
        Page<Product> page = productEmbeddingService.findActiveProducts(query, categorySlug, pageable);
        List<Product> products = page.getContent();

        Map<UUID, ProductReviewSnapshot> reviewByProductId = products.isEmpty()
                ? Map.of()
                : reviewRepository.summarizeByProductIds(
                                products.stream().map(Product::getId).toList()
                        ).stream()
                        .collect(Collectors.toMap(
                                ReviewRepository.ProductReviewAggregate::getProductId,
                                aggregate -> new ProductReviewSnapshot(
                                        aggregate.getAverageRating() != null ? aggregate.getAverageRating() : 0.0d,
                                        aggregate.getReviewCount() != null ? aggregate.getReviewCount().intValue() : 0
                                ),
                                (left, right) -> left
                        ));

        List<ProductSummaryDto> items = products.stream()
            .map(product -> toSummaryDto(product, reviewByProductId.get(product.getId())))
            .toList();

        return new ProductSearchPageDto(
            items,
            page.getNumber() + 1,
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.hasPrevious(),
            page.hasNext()
        );
    }

    @Transactional(readOnly = true)
    public ProductDetailsDto getProductBySlug(String slug, User user) {
        Product product = productRepository.findBySlugAndIsActiveTrue(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));

        List<Review> reviews = reviewRepository.findByProductIdOrderByCreatedAtDesc(product.getId());
        Review myReview = null;
        if (user != null) {
            myReview = reviewRepository.findByProductIdAndUserId(product.getId(), user.getId()).orElse(null);
        }

        boolean hasReviewed = myReview != null;
        boolean canReview = user != null && !hasReviewed && hasOrderedProduct(user, product);

        return toDetailsDto(product, reviews, canReview, hasReviewed, myReview != null ? myReview.getId().toString() : null);
    }

    @Transactional(readOnly = true)
    public List<ProductSummaryDto> getSimilarProducts(String slug, int size) {
        Product product = productRepository.findBySlugAndIsActiveTrue(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
        List<Product> similarProducts = productEmbeddingService.findSimilarProducts(product.getId(), size);

        return toSummaryDtos(similarProducts);
    }

    @Transactional
    public ProductDetailsDto createReview(UUID productId, User user, CreateProductReviewRequestDto request) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required to review products");
        }

        Product product = productRepository.findWithCategoriesById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
        if (!Boolean.TRUE.equals(product.getIsActive())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found");
        }

        if (!hasOrderedProduct(user, product)) {
            throw new BadRequestException("You can review this product only after purchasing it");
        }

        if (reviewRepository.existsByProductIdAndUserId(product.getId(), user.getId())) {
            throw new BadRequestException("You have already reviewed this product");
        }

        Review review = new Review();
        review.setProduct(product);
        review.setUser(user);
        review.setRating(request.rating().shortValue());
        review.setComment(request.comment().trim());
        reviewRepository.save(review);

        return getProductBySlug(product.getSlug(), user);
    }

    private ProductSummaryDto toSummaryDto(Product product, ProductReviewSnapshot reviewSnapshot) {
        ProductReviewSnapshot effectiveReviewSnapshot = reviewSnapshot != null
                ? reviewSnapshot
                : new ProductReviewSnapshot(0.0d, 0);
        String primaryImageId = resolvePrimaryImage(product.getImageId(), product.getImageGalleryIds());

        return new ProductSummaryDto(
                product.getId().toString(),
                product.getSlug(),
                extractPrimaryCategory(product),
                product.getName(),
                effectiveReviewSnapshot.averageRating(),
                effectiveReviewSnapshot.reviewCount(),
                product.getBasePrice(),
            primaryImageId
        );
    }

    private List<ProductSummaryDto> toSummaryDtos(List<Product> products) {
        Map<UUID, ProductReviewSnapshot> reviewByProductId = products.isEmpty()
                ? Map.of()
                : reviewRepository.summarizeByProductIds(
                                products.stream().map(Product::getId).toList()
                        ).stream()
                        .collect(Collectors.toMap(
                                ReviewRepository.ProductReviewAggregate::getProductId,
                                aggregate -> new ProductReviewSnapshot(
                                        aggregate.getAverageRating() != null ? aggregate.getAverageRating() : 0.0d,
                                        aggregate.getReviewCount() != null ? aggregate.getReviewCount().intValue() : 0
                                ),
                                (left, right) -> left
                        ));

        return products.stream()
                .map(product -> toSummaryDto(product, reviewByProductId.get(product.getId())))
                .toList();
    }

    private record ProductReviewSnapshot(double averageRating, int reviewCount) {
    }

    private ProductDetailsDto toDetailsDto(
            Product product,
            List<Review> reviews,
            boolean canReview,
            boolean hasReviewed,
            String reviewedReviewId
    ) {
        double averageRating = reviews.stream()
                .mapToInt(review -> review.getRating().intValue())
                .average()
                .orElse(0.0d);
        List<String> normalizedGallery = normalizeImageGallery(product.getImageId(), product.getImageGalleryIds());
        String primaryImageId = resolvePrimaryImage(product.getImageId(), normalizedGallery);

        return new ProductDetailsDto(
                product.getId().toString(),
                product.getSlug(),
                extractPrimaryCategory(product),
                product.getName(),
                averageRating,
                reviews.size(),
                canReview,
                hasReviewed,
                reviewedReviewId,
                product.getBasePrice(),
                product.getDescription(),
                product.getDetailedDescription(),
                primaryImageId,
                List.copyOf(normalizedGallery),
                reviews.stream().map(this::toProductReviewDto).toList()
        );
    }

    private String resolvePrimaryImage(String imageId, List<String> gallery) {
        String normalizedImageId = normalizeImageId(imageId);
        if (normalizedImageId != null) {
            return normalizedImageId;
        }

        return normalizeImageGallery(null, gallery).stream()
                .findFirst()
                .orElse("");
    }

    private List<String> normalizeImageGallery(String primaryImageId, List<String> imageGalleryIds) {
        List<String> normalizedGallery = new ArrayList<>();
        String normalizedPrimary = normalizeImageId(primaryImageId);

        if (normalizedPrimary != null) {
            normalizedGallery.add(normalizedPrimary);
        }

        if (imageGalleryIds != null) {
            for (String imageId : imageGalleryIds) {
                String normalizedImageId = normalizeImageId(imageId);
                if (normalizedImageId != null && !normalizedGallery.contains(normalizedImageId)) {
                    normalizedGallery.add(normalizedImageId);
                }
            }
        }

        return normalizedGallery;
    }

    private String normalizeImageId(String imageId) {
        if (imageId == null) {
            return null;
        }

        String trimmed = imageId.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private ProductReviewDto toProductReviewDto(Review review) {
        String reviewerName = (review.getUser().getFirstName() + " " + review.getUser().getLastName()).trim();
        return new ProductReviewDto(
                review.getId().toString(),
                review.getRating().intValue(),
                review.getComment(),
                reviewerName,
                review.getCreatedAt()
        );
    }

    private boolean hasOrderedProduct(User user, Product product) {
        return orderRepository.hasPurchasedProduct(user.getId(), product.getId(), REVIEW_ELIGIBLE_ORDER_STATUSES);
    }

    private String extractPrimaryCategory(Product product) {
        Category category = extractPrimaryCategoryEntity(product);
        return category != null ? category.getName() : "Uncategorized";
    }

    private Category extractPrimaryCategoryEntity(Product product) {
        if (product == null || product.getCategories() == null || product.getCategories().isEmpty()) {
            return null;
        }

        Comparator<Category> comparator = Comparator
                .comparingInt(this::categoryDepth)
                .reversed()
                .thenComparing(Category::getName, String.CASE_INSENSITIVE_ORDER);

        return product.getCategories().stream()
                .min(comparator)
                .orElse(null);
    }

    private int categoryDepth(Category category) {
        if (category == null) {
            return 0;
        }
        String path = category.getPath();
        if (path == null || path.isBlank()) {
            return 0;
        }
        return (int) path.chars().filter(ch -> ch == '.').count() + 1;
    }
}
