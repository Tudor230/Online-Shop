package org.endava.onlineshop.service.admin;

import org.endava.onlineshop.events.CategoryPathChangedEvent;
import org.endava.onlineshop.events.ProductCategoriesChangedEvent;
import org.endava.onlineshop.model.dto.admin.AdminCategoryCreateRequestDto;
import org.endava.onlineshop.model.dto.admin.AdminCategoryDto;
import org.endava.onlineshop.model.entities.Category;
import org.endava.onlineshop.repository.CategoryRepository;
import org.endava.onlineshop.repository.ProductRepository;
import org.endava.onlineshop.security.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminCategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private AdminAuditLogService auditLogService;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AdminCategoryService adminCategoryService;

    private UUID categoryId;
    private UUID adminId;

    @BeforeEach
    void setUp() {
        categoryId = UUID.randomUUID();
        adminId = UUID.randomUUID();
        lenient().when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(adminId));
        lenient().when(securityUtils.getCurrentUserEmail()).thenReturn(Optional.of("admin@test.com"));
    }

    @Test
    void createCategoryShouldPublishCategoryPathChangedEvent() {
        AdminCategoryCreateRequestDto request = new AdminCategoryCreateRequestDto(null, "Phones", "phones");

        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category c = invocation.getArgument(0);
            c.setId(categoryId);
            return c;
        });

        AdminCategoryDto result = adminCategoryService.createCategory(request);

        assertThat(result.id()).isEqualTo(categoryId);
        verify(eventPublisher).publishEvent((Object) argThat(e ->
                e instanceof CategoryPathChangedEvent ev && ev.categoryId().equals(categoryId)));
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void createCategoryShouldNotCheckSlugUniqueness() {
        AdminCategoryCreateRequestDto request = new AdminCategoryCreateRequestDto(null, "Chargers", "chargers");

        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category c = invocation.getArgument(0);
            c.setId(categoryId);
            return c;
        });

        adminCategoryService.createCategory(request);

        verify(categoryRepository, never()).findByParentIdAndSlug(any(), any());
    }

    @Test
    void updateCategoryWithSlugChangeShouldPublishCategoryPathChangedEvent() {
        Category existing = buildCategory(categoryId, null, "Phones", "phones-old");
        AdminCategoryCreateRequestDto request = new AdminCategoryCreateRequestDto(null, "Phones", "phones-new");

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(existing));
        when(categoryRepository.save(any(Category.class))).thenReturn(existing);

        adminCategoryService.updateCategory(categoryId, request);

        verify(eventPublisher).publishEvent((Object) argThat(e ->
                e instanceof CategoryPathChangedEvent ev && ev.categoryId().equals(categoryId)));
    }

    @Test
    void updateCategoryWithParentChangeShouldPublishCategoryPathChangedEvent() {
        UUID newParentId = UUID.randomUUID();
        Category existing = buildCategory(categoryId, UUID.randomUUID(), "Chargers", "chargers");
        AdminCategoryCreateRequestDto request = new AdminCategoryCreateRequestDto(newParentId, "Chargers", "chargers");

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(existing));
        when(categoryRepository.save(any(Category.class))).thenReturn(existing);

        adminCategoryService.updateCategory(categoryId, request);

        verify(eventPublisher).publishEvent((Object) argThat(e ->
                e instanceof CategoryPathChangedEvent ev && ev.categoryId().equals(categoryId)));
    }

    @Test
    void updateCategoryWithNameOnlyShouldNotPublishEvent() {
        Category existing = buildCategory(categoryId, null, "Old Name", "phones");
        AdminCategoryCreateRequestDto request = new AdminCategoryCreateRequestDto(null, "New Name", null);

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(existing));
        when(categoryRepository.save(any(Category.class))).thenReturn(existing);

        adminCategoryService.updateCategory(categoryId, request);

        verify(eventPublisher, never()).publishEvent(any(CategoryPathChangedEvent.class));
    }

    @Test
    void updateCategoryWithNoPathChangesShouldNotPublishEvent() {
        Category existing = buildCategory(categoryId, null, "Phones", "phones");
        AdminCategoryCreateRequestDto request = new AdminCategoryCreateRequestDto(null, "Phones", "phones");

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(existing));
        when(categoryRepository.save(any(Category.class))).thenReturn(existing);

        adminCategoryService.updateCategory(categoryId, request);

        verify(eventPublisher, never()).publishEvent(any(CategoryPathChangedEvent.class));
    }

    @Test
    void updateCategoryShouldNotCheckSlugUniqueness() {
        Category existing = buildCategory(categoryId, null, "Phones", "phones-old");
        AdminCategoryCreateRequestDto request = new AdminCategoryCreateRequestDto(null, "Phones", "phones-new");

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(existing));
        when(categoryRepository.save(any(Category.class))).thenReturn(existing);

        adminCategoryService.updateCategory(categoryId, request);

        verify(categoryRepository, never()).findByParentIdAndSlug(any(), any());
    }

    @Test
    void deleteCategoryShouldPublishProductCategoriesChangedEventForEachProduct() {
        UUID productId1 = UUID.randomUUID();
        UUID productId2 = UUID.randomUUID();

        when(categoryRepository.existsById(categoryId)).thenReturn(true);
        when(productRepository.findProductIdsByCategoryId(eq(categoryId), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(productId1, productId2)));

        adminCategoryService.deleteCategory(categoryId);

        verify(categoryRepository).deleteById(categoryId);
        verify(eventPublisher).publishEvent((Object) argThat(e ->
                e instanceof ProductCategoriesChangedEvent ev && ev.productId().equals(productId1)));
        verify(eventPublisher).publishEvent((Object) argThat(e ->
                e instanceof ProductCategoriesChangedEvent ev && ev.productId().equals(productId2)));
    }

    @Test
    void deleteCategoryShouldThrowWhenNotFound() {
        when(categoryRepository.existsById(categoryId)).thenReturn(false);

        assertThatThrownBy(() -> adminCategoryService.deleteCategory(categoryId))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void deleteCategoryShouldNotPublishEventsWhenNoProducts() {
        when(categoryRepository.existsById(categoryId)).thenReturn(true);
        when(productRepository.findProductIdsByCategoryId(eq(categoryId), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of()));

        adminCategoryService.deleteCategory(categoryId);

        verify(categoryRepository).deleteById(categoryId);
        verify(eventPublisher, never()).publishEvent(any(ProductCategoriesChangedEvent.class));
    }

    private Category buildCategory(UUID id, UUID parentId, String name, String slug) {
        Category category = new Category();
        category.setId(id);
        category.setParentId(parentId);
        category.setName(name);
        category.setSlug(slug);
        return category;
    }
}
