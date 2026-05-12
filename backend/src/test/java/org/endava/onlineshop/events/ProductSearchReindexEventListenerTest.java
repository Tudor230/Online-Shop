package org.endava.onlineshop.events;

import org.endava.onlineshop.service.ProductSearchIndexMaintenanceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class ProductSearchReindexEventListenerTest {

    @Mock
    private ProductSearchIndexMaintenanceService productSearchIndexMaintenanceService;

    @InjectMocks
    private ProductSearchReindexEventListener listener;

    @Test
    void shouldCallReindexProductOnProductCategoriesChanged() {
        UUID productId = UUID.randomUUID();
        ProductCategoriesChangedEvent event = new ProductCategoriesChangedEvent(productId);

        listener.onProductCategoriesChanged(event);

        verify(productSearchIndexMaintenanceService).reindexProduct(productId);
        verifyNoMoreInteractions(productSearchIndexMaintenanceService);
    }

    @Test
    void shouldCallReindexProductsForCategoryOnCategoryPathChanged() {
        UUID categoryId = UUID.randomUUID();
        CategoryPathChangedEvent event = new CategoryPathChangedEvent(categoryId);

        listener.onCategoryPathChanged(event);

        verify(productSearchIndexMaintenanceService).reindexProductsForCategory(categoryId);
        verifyNoMoreInteractions(productSearchIndexMaintenanceService);
    }

    @Test
    void shouldCallUpdateProductEmbeddingOnProductDetailsChanged() {
        UUID productId = UUID.randomUUID();
        ProductDetailsChangedEvent event = new ProductDetailsChangedEvent(productId);

        listener.onProductDetailsChanged(event);

        verify(productSearchIndexMaintenanceService).updateProductEmbedding(productId);
        verifyNoMoreInteractions(productSearchIndexMaintenanceService);
    }
}
