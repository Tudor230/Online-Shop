package org.endava.onlineshop.service;

import lombok.RequiredArgsConstructor;
import org.endava.onlineshop.exception.BadRequestException;
import org.endava.onlineshop.model.dto.product.*;
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

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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
    public ProductSearchPageDto getProducts(String query, Pageable pageable) {
        Page<Product> page = productEmbeddingService.findActiveProducts(query, pageable);
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

    @Transactional
    public ProductDetailsDto createReview(String slug, User user, CreateProductReviewRequestDto request) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required to review products");
        }

        Product product = productRepository.findBySlugAndIsActiveTrue(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));

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

        return getProductBySlug(slug, user);
    }

    private ProductSummaryDto toSummaryDto(Product product, ProductReviewSnapshot reviewSnapshot) {
        ProductReviewSnapshot effectiveReviewSnapshot = reviewSnapshot != null
                ? reviewSnapshot
                : new ProductReviewSnapshot(0.0d, 0);

        return new ProductSummaryDto(
                product.getSlug(),
                extractPrimaryCategory(product),
                product.getName(),
                effectiveReviewSnapshot.averageRating(),
                effectiveReviewSnapshot.reviewCount(),
                product.getBasePrice(),
                product.getImageId()
        );
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

        return new ProductDetailsDto(
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
                product.getImageId(),
                List.copyOf(product.getImageGalleryIds()),
                reviews.stream().map(this::toProductReviewDto).toList()
        );
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
        return product.getCategories().stream()
                .map(Category::getName)
                .sorted()
                .findFirst()
                .orElse("Uncategorized");
    }
}
