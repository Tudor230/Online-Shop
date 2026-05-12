package org.endava.onlineshop.service.admin;

import org.endava.onlineshop.events.ProductCategoriesChangedEvent;
import org.endava.onlineshop.events.ProductDetailsChangedEvent;
import org.endava.onlineshop.exception.BadRequestException;
import org.endava.onlineshop.model.dto.admin.*;
import org.endava.onlineshop.model.entities.Category;
import org.endava.onlineshop.model.entities.Product;
import org.endava.onlineshop.model.entities.ProductInventory;
import org.endava.onlineshop.repository.CategoryRepository;
import org.endava.onlineshop.repository.ProductInventoryRepository;
import org.endava.onlineshop.repository.ProductRepository;
import org.endava.onlineshop.security.SecurityUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class AdminProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductInventoryRepository productInventoryRepository;
    private final AdminAuditLogService auditLogService;
    private final SecurityUtils securityUtils;
    private final ApplicationEventPublisher eventPublisher;

    public AdminProductService(
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            ProductInventoryRepository productInventoryRepository,
            AdminAuditLogService auditLogService,
            SecurityUtils securityUtils,
            ApplicationEventPublisher eventPublisher
    ) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.productInventoryRepository = productInventoryRepository;
        this.auditLogService = auditLogService;
        this.securityUtils = securityUtils;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public Page<AdminProductListDto> getProducts(Pageable pageable) {
        return productRepository.findAll(pageable).map(this::toListDto);
    }

    @Transactional(readOnly = true)
    public AdminProductDetailDto getProduct(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
        return toDetailDto(product);
    }

    @Transactional
    public AdminProductDetailDto createProduct(AdminProductCreateRequestDto request) {
        if (productRepository.existsBySlug(request.slug())) {
            throw new BadRequestException("Product slug already exists");
        }

        Product product = new Product();
        product.setSku(request.sku());
        product.setName(request.name());
        product.setSlug(request.slug());
        product.setDescription(request.description());
        product.setBasePrice(request.basePrice());
        product.setImageId(request.imagePlaceholder());
        product.setImageGalleryIds(request.imageGallery() != null ? request.imageGallery() : new java.util.ArrayList<>());

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
        return toDetailDto(savedProduct);
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
        if (request.basePrice() != null) product.setBasePrice(request.basePrice());
        if (request.isActive() != null) product.setIsActive(request.isActive());
        if (request.imagePlaceholder() != null) product.setImageId(request.imagePlaceholder());
        if (request.imageGallery() != null) product.setImageGalleryIds(request.imageGallery());
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

        return toDetailDto(savedProduct);
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

    private AdminProductListDto toListDto(Product product) {
        Integer qty = product.getInventory() != null ? product.getInventory().getQuantityAvailable() : 0;
        Integer threshold = product.getInventory() != null ? product.getInventory().getLowStockThreshold() : 5;
        List<String> categories = product.getCategories().stream().map(Category::getName).toList();
        return new AdminProductListDto(
                product.getId(), product.getSku(), product.getName(), product.getSlug(),
                product.getBasePrice(), product.getIsActive(), product.getRating(),
                product.getReviewCount(), product.getImageId(), qty, threshold,
                categories, product.getCreatedAt(), product.getUpdatedAt()
        );
    }

    private AdminProductDetailDto toDetailDto(Product product) {
        List<AdminCategoryDto> categories = product.getCategories().stream()
                .map(c -> new AdminCategoryDto(c.getId(), c.getParentId(), c.getName(), c.getSlug()))
                .toList();
        AdminInventoryDto inventory = product.getInventory() != null
                ? new AdminInventoryDto(product.getInventory().getQuantityAvailable(), product.getInventory().getLowStockThreshold())
                : new AdminInventoryDto(0, 5);
        return new AdminProductDetailDto(
                product.getId(), product.getSku(), product.getName(), product.getSlug(),
                product.getDescription(), product.getBasePrice(), product.getIsActive(),
                product.getRating(), product.getReviewCount(), product.getImageId(),
                List.copyOf(product.getImageGalleryIds()), categories, inventory,
                product.getCreatedAt(), product.getUpdatedAt()
        );
    }
}
