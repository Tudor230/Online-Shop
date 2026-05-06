package org.endava.onlineshop.events;

import org.endava.onlineshop.service.ProductSearchIndexMaintenanceService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class ProductSearchReindexEventListener {

    private final ProductSearchIndexMaintenanceService productSearchIndexMaintenanceService;

    public ProductSearchReindexEventListener(ProductSearchIndexMaintenanceService productSearchIndexMaintenanceService) {
        this.productSearchIndexMaintenanceService = productSearchIndexMaintenanceService;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onProductCategoriesChanged(ProductCategoriesChangedEvent event) {
        productSearchIndexMaintenanceService.reindexProduct(event.productId());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCategoryPathChanged(CategoryPathChangedEvent event) {
        productSearchIndexMaintenanceService.reindexProductsForCategory(event.categoryId());
    }
}

