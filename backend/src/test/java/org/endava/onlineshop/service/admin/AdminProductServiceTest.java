package org.endava.onlineshop.service.admin;

import org.endava.onlineshop.events.ProductCategoriesChangedEvent;
import org.endava.onlineshop.events.ProductDetailsChangedEvent;
import org.endava.onlineshop.model.dto.admin.AdminProductDetailDto;
import org.endava.onlineshop.model.dto.admin.AdminProductUpdateRequestDto;
import org.endava.onlineshop.model.entities.Category;
import org.endava.onlineshop.model.entities.Product;
import org.endava.onlineshop.model.entities.ProductInventory;
import org.endava.onlineshop.repository.CategoryRepository;
import org.endava.onlineshop.repository.ProductInventoryRepository;
import org.endava.onlineshop.repository.ProductRepository;
import org.endava.onlineshop.security.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductInventoryRepository productInventoryRepository;

    @Mock
    private AdminAuditLogService auditLogService;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AdminProductService adminProductService;

    private UUID productId;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(UUID.randomUUID()));
        when(securityUtils.getCurrentUserEmail()).thenReturn(Optional.of("admin@test.com"));
    }

    @Test
    void updateProductWithNameChangeShouldPublishProductDetailsChangedEvent() {
        Product existing = buildProduct("Old Name", "desc", "slug");
        AdminProductUpdateRequestDto request = buildUpdateRequest("New Name", "desc", null);

        when(productRepository.findById(productId)).thenReturn(Optional.of(existing));
        when(productRepository.save(any(Product.class))).thenReturn(existing);

        adminProductService.updateProduct(productId, request);

        verify(eventPublisher).publishEvent(argThat(e ->
                e instanceof ProductDetailsChangedEvent ev && ev.productId().equals(productId)));
        verify(eventPublisher, never()).publishEvent(any(ProductCategoriesChangedEvent.class));
    }

    @Test
    void updateProductWithDescriptionChangeShouldPublishProductDetailsChangedEvent() {
        Product existing = buildProduct("Name", "Old desc", "slug");
        AdminProductUpdateRequestDto request = buildUpdateRequest("Name", "New desc", null);

        when(productRepository.findById(productId)).thenReturn(Optional.of(existing));
        when(productRepository.save(any(Product.class))).thenReturn(existing);

        adminProductService.updateProduct(productId, request);

        verify(eventPublisher).publishEvent(argThat(e ->
                e instanceof ProductDetailsChangedEvent ev && ev.productId().equals(productId)));
        verify(eventPublisher, never()).publishEvent(any(ProductCategoriesChangedEvent.class));
    }

    @Test
    void updateProductWithCategoryChangeShouldPublishProductCategoriesChangedEvent() {
        Product existing = buildProduct("Name", "desc", "slug");
        AdminProductUpdateRequestDto request = buildUpdateRequest("Name", "desc", List.of(UUID.randomUUID()));

        when(productRepository.findById(productId)).thenReturn(Optional.of(existing));
        when(categoryRepository.findAllById(anyList())).thenReturn(List.of(new Category()));
        when(productRepository.save(any(Product.class))).thenReturn(existing);

        adminProductService.updateProduct(productId, request);

        verify(eventPublisher).publishEvent(argThat(e ->
                e instanceof ProductCategoriesChangedEvent ev && ev.productId().equals(productId)));
        verify(eventPublisher, never()).publishEvent(any(ProductDetailsChangedEvent.class));
    }

    @Test
    void updateProductWithBothNameAndCategoryChangeShouldPublishBothEvents() {
        Product existing = buildProduct("Old Name", "desc", "slug");
        AdminProductUpdateRequestDto request = buildUpdateRequest("New Name", "desc", List.of(UUID.randomUUID()));

        when(productRepository.findById(productId)).thenReturn(Optional.of(existing));
        when(categoryRepository.findAllById(anyList())).thenReturn(List.of(new Category()));
        when(productRepository.save(any(Product.class))).thenReturn(existing);

        adminProductService.updateProduct(productId, request);

        verify(eventPublisher).publishEvent(argThat(e ->
                e instanceof ProductCategoriesChangedEvent ev && ev.productId().equals(productId)));
        verify(eventPublisher).publishEvent(argThat(e ->
                e instanceof ProductDetailsChangedEvent ev && ev.productId().equals(productId)));
    }

    @Test
    void updateProductWithNoMeaningfulChangesShouldNotPublishEvents() {
        Product existing = buildProduct("Name", "desc", "slug");
        AdminProductUpdateRequestDto request = new AdminProductUpdateRequestDto(
                "sku", "Name", "slug", "desc", null, null, null, null, null, null, null);

        when(productRepository.findById(productId)).thenReturn(Optional.of(existing));
        when(productRepository.save(any(Product.class))).thenReturn(existing);

        adminProductService.updateProduct(productId, request);

        verify(eventPublisher, never()).publishEvent(any(ProductCategoriesChangedEvent.class));
        verify(eventPublisher, never()).publishEvent(any(ProductDetailsChangedEvent.class));
    }

    @Test
    void updateProductShouldReturnUpdatedDto() {
        Product existing = buildProduct("Name", "desc", "slug");
        AdminProductUpdateRequestDto request = new AdminProductUpdateRequestDto(
                null, "Updated Name", null, null, null, null, null, null, null, null, null);

        when(productRepository.findById(productId)).thenReturn(Optional.of(existing));
        when(productRepository.save(any(Product.class))).thenReturn(existing);

        AdminProductDetailDto result = adminProductService.updateProduct(productId, request);

        assertThat(result.name()).isEqualTo("Updated Name");
        verify(eventPublisher).publishEvent(any(ProductDetailsChangedEvent.class));
    }

    private Product buildProduct(String name, String description, String slug) {
        Product product = new Product();
        product.setId(productId);
        product.setName(name);
        product.setDescription(description);
        product.setSlug(slug);
        product.setSku("SKU-001");
        product.setBasePrice(new BigDecimal("99.99"));
        product.setIsActive(true);
        product.setImageId("placeholder");
        product.setRating(0.0);
        product.setReviewCount(0);

        ProductInventory inventory = new ProductInventory();
        inventory.setQuantityAvailable(10);
        inventory.setLowStockThreshold(5);
        product.setInventory(inventory);

        return product;
    }

    private AdminProductUpdateRequestDto buildUpdateRequest(String name, String description, List<UUID> categoryIds) {
        return new AdminProductUpdateRequestDto(
                null, name, null, description, null, null, categoryIds, null, null, null, null);
    }
}
