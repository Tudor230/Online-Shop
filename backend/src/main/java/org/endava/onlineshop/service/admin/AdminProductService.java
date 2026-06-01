package org.endava.onlineshop.service.admin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.endava.onlineshop.events.ProductCategoriesChangedEvent;
import org.endava.onlineshop.events.ProductDetailsChangedEvent;
import org.endava.onlineshop.exception.BadRequestException;
import org.endava.onlineshop.model.dto.admin.AdminCategoryDto;
import org.endava.onlineshop.model.dto.admin.AdminInventoryDto;
import org.endava.onlineshop.model.dto.admin.AdminProductCreateRequestDto;
import org.endava.onlineshop.model.dto.admin.AdminProductDetailDto;
import org.endava.onlineshop.model.dto.admin.AdminProductListDto;
import org.endava.onlineshop.model.dto.admin.AdminProductUpdateRequestDto;
import org.endava.onlineshop.model.dto.admin.AdminUploadedProductImageDto;
import org.endava.onlineshop.model.entities.Category;
import org.endava.onlineshop.model.entities.Product;
import org.endava.onlineshop.model.entities.ProductInventory;
import org.endava.onlineshop.repository.CategoryRepository;
import org.endava.onlineshop.repository.ProductInventoryRepository;
import org.endava.onlineshop.repository.ProductRepository;
import org.endava.onlineshop.repository.ReviewRepository;
import org.endava.onlineshop.security.SecurityUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductInventoryRepository productInventoryRepository;
    private final ReviewRepository reviewRepository;
    private final ProductImageStorageService productImageStorageService;
    private final AdminAuditLogService auditLogService;
    private final SecurityUtils securityUtils;
    private final ApplicationEventPublisher eventPublisher;

    public AdminProductService(
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            ProductInventoryRepository productInventoryRepository,
            ReviewRepository reviewRepository,
            ProductImageStorageService productImageStorageService,
            AdminAuditLogService auditLogService,
            SecurityUtils securityUtils,
            ApplicationEventPublisher eventPublisher
    ) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.productInventoryRepository = productInventoryRepository;
        this.reviewRepository = reviewRepository;
        this.productImageStorageService = productImageStorageService;
        this.auditLogService = auditLogService;
        this.securityUtils = securityUtils;
        this.eventPublisher = eventPublisher;
    }

    public List<AdminUploadedProductImageDto> uploadProductImages(List<MultipartFile> files) {
        return productImageStorageService.uploadProductImages(files);
    }

    @Transactional(readOnly = true)
    public Page<AdminProductListDto> getProducts(Pageable pageable) {
        Page<Product> productPage = productRepository.findAll(pageable);
        Map<UUID, ProductReviewSnapshot> reviewSnapshots = summarizeReviewsByProductId(
                productPage.getContent().stream().map(Product::getId).toList()
        );
        return productPage.map(product -> toListDto(
                product,
                reviewSnapshots.getOrDefault(product.getId(), ProductReviewSnapshot.empty())
        ));
    }

    @Transactional(readOnly = true)
    public AdminProductDetailDto getProduct(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
        ProductReviewSnapshot reviewSnapshot = summarizeReviewsByProductId(List.of(product.getId()))
                .getOrDefault(product.getId(), ProductReviewSnapshot.empty());
        return toDetailDto(product, reviewSnapshot);
    }

    @Transactional
    public AdminProductDetailDto createProduct(AdminProductCreateRequestDto request) {
        if (productRepository.existsBySlug(request.slug())) {
            throw new BadRequestException("Product slug already exists");
        }

        String primaryImageId = requirePrimaryImage(request.imagePlaceholder());
        List<String> normalizedGallery = normalizeImageGallery(primaryImageId, request.imageGallery());

        Product product = new Product();
        product.setSku(request.sku());
        product.setName(request.name());
        product.setSlug(request.slug());
        product.setDescription(request.description());
        product.setDetailedDescription(request.detailedDescription());
        product.setBasePrice(request.basePrice());
        product.setImageId(primaryImageId);
        product.setImageGalleryIds(new ArrayList<>(normalizedGallery));

        if (request.categoryIds() != null && !request.categoryIds().isEmpty()) {
            List<Category> categories = categoryRepository.findAllById(request.categoryIds());
            product.setCategories(new java.util.HashSet<>(categories));
        }

        ProductInventory inventory = new ProductInventory();
        inventory.setQuantityAvailable(request.initialQuantity() != null ? request.initialQuantity() : 0);
        inventory.setLowStockThreshold(request.lowStockThreshold() != null ? request.lowStockThreshold() : 5);

        product.setInventory(inventory);
        Product savedProduct = productRepository.save(product);

        audit("CREATE", "PRODUCT", savedProduct.getId().toString(), "Created product " + savedProduct.getName());
        return toDetailDto(savedProduct, ProductReviewSnapshot.empty());
    }

    @Transactional
    public AdminProductDetailDto updateProduct(UUID id, AdminProductUpdateRequestDto request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));

        boolean detailsChanged = false;
        boolean categoriesChanged = false;

        if (request.sku() != null) product.setSku(request.sku());
        if (request.name() != null && !request.name().equals(product.getName())) {
            product.setName(request.name());
            detailsChanged = true;
        }
        if (request.slug() != null && !request.slug().equals(product.getSlug())) {
            if (productRepository.existsBySlug(request.slug())) {
                throw new BadRequestException("Product slug already exists");
            }
            product.setSlug(request.slug());
        }
        if (request.description() != null && !request.description().equals(product.getDescription())) {
            product.setDescription(request.description());
            detailsChanged = true;
        }
        if (request.detailedDescription() != null && !request.detailedDescription().equals(product.getDetailedDescription())) {
            product.setDetailedDescription(request.detailedDescription());
            detailsChanged = true;
        }
        if (request.basePrice() != null) product.setBasePrice(request.basePrice());
        if (request.isActive() != null) product.setIsActive(request.isActive());
        if (request.imagePlaceholder() != null || request.imageGallery() != null) {
            String requestedPrimaryImage = request.imagePlaceholder() != null
                    ? requirePrimaryImage(request.imagePlaceholder())
                    : requirePrimaryImage(product.getImageId());
            List<String> normalizedGallery = normalizeImageGallery(
                    requestedPrimaryImage,
                    request.imageGallery() != null ? request.imageGallery() : product.getImageGalleryIds()
            );

            if (!Objects.equals(product.getImageId(), requestedPrimaryImage)
                    || !Objects.equals(product.getImageGalleryIds(), normalizedGallery)) {
                detailsChanged = true;
            }

            product.setImageId(requestedPrimaryImage);
            product.setImageGalleryIds(new ArrayList<>(normalizedGallery));
        }
        if (request.categoryIds() != null) {
            List<Category> categories = categoryRepository.findAllById(request.categoryIds());
            product.setCategories(new java.util.HashSet<>(categories));
            categoriesChanged = true;
        }

        ProductInventory inventory = product.getInventory();
        if (inventory == null) {
            inventory = new ProductInventory();
            product.setInventory(inventory);
        }
        if (request.quantityAvailable() != null) inventory.setQuantityAvailable(request.quantityAvailable());
        if (request.lowStockThreshold() != null) inventory.setLowStockThreshold(request.lowStockThreshold());

        Product savedProduct = productRepository.save(product);
        audit("UPDATE", "PRODUCT", savedProduct.getId().toString(), "Updated product " + savedProduct.getName());

        if (categoriesChanged) {
            eventPublisher.publishEvent(new ProductCategoriesChangedEvent(savedProduct.getId()));
        }
        if (detailsChanged) {
            eventPublisher.publishEvent(new ProductDetailsChangedEvent(savedProduct.getId()));
        }

        ProductReviewSnapshot reviewSnapshot = summarizeReviewsByProductId(List.of(savedProduct.getId()))
                .getOrDefault(savedProduct.getId(), ProductReviewSnapshot.empty());
        return toDetailDto(savedProduct, reviewSnapshot);
    }

    @Transactional
    public void deleteProduct(UUID id) {
        if (!productRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found");
        }
        audit("DELETE", "PRODUCT", id.toString(), "Deleted product " + id);
        productRepository.deleteById(id);
    }

    @Transactional
    public void bulkDeleteProducts(List<UUID> ids) {
        audit("BULK_DELETE", "PRODUCT", "MULTIPLE", "Bulk deleted " + ids.size() + " products: " + ids.toString());
        productRepository.deleteAllById(ids);
    }

    @Transactional
    public void bulkActivateProducts(List<UUID> ids) {
        List<Product> products = productRepository.findAllById(ids);
        products.forEach(p -> p.setIsActive(true));
        productRepository.saveAll(products);
        audit("BULK_ACTIVATE", "PRODUCT", "MULTIPLE", "Bulk activated " + ids.size() + " products: " + ids.toString());
    }

    @Transactional
    public void bulkDeactivateProducts(List<UUID> ids) {
        List<Product> products = productRepository.findAllById(ids);
        products.forEach(p -> p.setIsActive(false));
        productRepository.saveAll(products);
        audit("BULK_DEACTIVATE", "PRODUCT", "MULTIPLE", "Bulk deactivated " + ids.size() + " products: " + ids.toString());
    }

    private void audit(String action, String entityType, String entityId, String details) {
        auditLogService.log(
                securityUtils.getCurrentUserId().orElse(null),
                securityUtils.getCurrentUserEmail().orElse("system"),
                action, entityType, entityId, details
        );
    }

    private AdminProductListDto toListDto(Product product, ProductReviewSnapshot reviewSnapshot) {
        Integer qty = product.getInventory() != null ? product.getInventory().getQuantityAvailable() : 0;
        Integer threshold = product.getInventory() != null ? product.getInventory().getLowStockThreshold() : 5;
        List<String> categories = product.getCategories().stream().map(Category::getName).toList();
        String primaryImageId = resolvePrimaryImage(product.getImageId(), product.getImageGalleryIds());
        return new AdminProductListDto(
                product.getId(), product.getSku(), product.getName(), product.getSlug(),
            product.getBasePrice(), product.getIsActive(), reviewSnapshot.averageRating(),
            reviewSnapshot.reviewCount(), primaryImageId, qty, threshold,
                categories, product.getCreatedAt(), product.getUpdatedAt()
        );
    }

    private AdminProductDetailDto toDetailDto(Product product, ProductReviewSnapshot reviewSnapshot) {
        List<AdminCategoryDto> categories = product.getCategories().stream()
                .map(c -> new AdminCategoryDto(c.getId(), c.getParentId(), c.getName(), c.getSlug()))
                .toList();
        AdminInventoryDto inventory = product.getInventory() != null
                ? new AdminInventoryDto(product.getInventory().getQuantityAvailable(), product.getInventory().getLowStockThreshold())
                : new AdminInventoryDto(0, 5);
        List<String> normalizedGallery = normalizeImageGallery(product.getImageId(), product.getImageGalleryIds());
        return new AdminProductDetailDto(
                product.getId(), product.getSku(), product.getName(), product.getSlug(),
                product.getDescription(), product.getDetailedDescription(), product.getBasePrice(), product.getIsActive(),
                reviewSnapshot.averageRating(), reviewSnapshot.reviewCount(), resolvePrimaryImage(product.getImageId(), normalizedGallery),
                List.copyOf(normalizedGallery), categories, inventory,
                product.getCreatedAt(), product.getUpdatedAt()
        );
    }

    private String requirePrimaryImage(String imageId) {
        String normalizedImageId = normalizeImageId(imageId);
        if (normalizedImageId == null) {
            throw new BadRequestException("Primary image is required");
        }
        return normalizedImageId;
    }

    private String resolvePrimaryImage(String imageId, List<String> gallery) {
        String normalizedImageId = normalizeImageId(imageId);
        if (normalizedImageId != null) {
            return normalizedImageId;
        }

        return normalizeImageGallery(null, gallery).stream()
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Primary image is required"));
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

    private Map<UUID, ProductReviewSnapshot> summarizeReviewsByProductId(List<UUID> productIds) {
        if (productIds.isEmpty()) {
            return Map.of();
        }

        return reviewRepository.summarizeByProductIds(productIds).stream()
                .collect(Collectors.toMap(
                        ReviewRepository.ProductReviewAggregate::getProductId,
                        aggregate -> new ProductReviewSnapshot(
                                aggregate.getAverageRating() != null ? aggregate.getAverageRating() : 0.0d,
                                aggregate.getReviewCount() != null ? aggregate.getReviewCount().intValue() : 0
                        ),
                        (left, right) -> left
                ));
    }

    private record ProductReviewSnapshot(double averageRating, int reviewCount) {
        private static ProductReviewSnapshot empty() {
            return new ProductReviewSnapshot(0.0d, 0);
        }
    }
}
