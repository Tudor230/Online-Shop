package org.endava.onlineshop.service.admin;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.endava.onlineshop.events.ProductCategoriesChangedEvent;
import org.endava.onlineshop.events.ProductDetailsChangedEvent;
import org.endava.onlineshop.model.dto.admin.AdminProductDetailDto;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class AdminProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ProductImageStorageService productImageStorageService;

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
        lenient().when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(UUID.randomUUID()));
        lenient().when(securityUtils.getCurrentUserEmail()).thenReturn(Optional.of("admin@test.com"));
    }

    @Test
    void updateProductWithNameChangeShouldPublishProductDetailsChangedEvent() {
        Product existing = buildProduct("Old Name", "desc", "slug");
        AdminProductUpdateRequestDto request = buildUpdateRequest("New Name", "desc", null);

        when(productRepository.findById(productId)).thenReturn(Optional.of(existing));
        when(productRepository.save(any(Product.class))).thenReturn(existing);

        adminProductService.updateProduct(productId, request);

        verify(eventPublisher).publishEvent((Object) argThat(e ->
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

        verify(eventPublisher).publishEvent((Object) argThat(e ->
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

        verify(eventPublisher).publishEvent((Object) argThat(e ->
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

        verify(eventPublisher).publishEvent((Object) argThat(e ->
                e instanceof ProductCategoriesChangedEvent ev && ev.productId().equals(productId)));
        verify(eventPublisher).publishEvent((Object) argThat(e ->
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

    @Test
    void updateProductShouldMovePrimaryImageToFrontOfGallery() {
        Product existing = buildProduct("Name", "desc", "slug");
        existing.setImageId("gallery-1");
        existing.setImageGalleryIds(List.of("gallery-1", "gallery-2", "gallery-3"));
        AdminProductUpdateRequestDto request = new AdminProductUpdateRequestDto(
                null, null, null, null, null, null, null, "gallery-3", List.of("gallery-1", "gallery-2", "gallery-3"), null, null);

        when(productRepository.findById(productId)).thenReturn(Optional.of(existing));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminProductDetailDto result = adminProductService.updateProduct(productId, request);

        assertThat(result.imagePlaceholder()).isEqualTo("gallery-3");
        assertThat(result.imageGallery()).containsExactly("gallery-3", "gallery-1", "gallery-2");
        verify(eventPublisher).publishEvent(any(ProductDetailsChangedEvent.class));
    }

    @Test
    void uploadProductImagesShouldDelegateToStorageService() {
        List<AdminUploadedProductImageDto> uploadedImages = List.of(
                new AdminUploadedProductImageDto("image-1", "https://example.com/image-1", "shoe.png")
        );

        when(productImageStorageService.uploadProductImages(anyList())).thenReturn(uploadedImages);

        List<AdminUploadedProductImageDto> result = adminProductService.uploadProductImages(List.of());

        assertThat(result).isEqualTo(uploadedImages);
        verify(productImageStorageService).uploadProductImages(anyList());
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
        product.setImageGalleryIds(List.of("placeholder"));

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
