package org.endava.onlineshop.service.admin;

import org.endava.onlineshop.exception.BadRequestException;
import org.endava.onlineshop.model.dto.admin.*;
import org.endava.onlineshop.model.entities.Category;
import org.endava.onlineshop.model.entities.Product;
import org.endava.onlineshop.model.entities.ProductInventory;
import org.endava.onlineshop.repository.CategoryRepository;
import org.endava.onlineshop.repository.ProductInventoryRepository;
import org.endava.onlineshop.repository.ProductRepository;
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

    public AdminProductService(
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            ProductInventoryRepository productInventoryRepository
    ) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.productInventoryRepository = productInventoryRepository;
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
        product.setImagePlaceholder(request.imagePlaceholder());
        product.setImageGallery(request.imageGallery() != null ? request.imageGallery() : new java.util.ArrayList<>());

        if (request.categoryIds() != null && !request.categoryIds().isEmpty()) {
            List<Category> categories = categoryRepository.findAllById(request.categoryIds());
            product.setCategories(new java.util.HashSet<>(categories));
        }

        Product savedProduct = productRepository.save(product);

        ProductInventory inventory = new ProductInventory();
        inventory.setProductId(savedProduct.getId());
        inventory.setQuantityAvailable(request.initialQuantity() != null ? request.initialQuantity() : 0);
        inventory.setLowStockThreshold(request.lowStockThreshold() != null ? request.lowStockThreshold() : 5);
        productInventoryRepository.save(inventory);

        savedProduct.setInventory(inventory);
        return toDetailDto(savedProduct);
    }

    @Transactional
    public AdminProductDetailDto updateProduct(UUID id, AdminProductUpdateRequestDto request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));

        if (request.sku() != null) product.setSku(request.sku());
        if (request.name() != null) product.setName(request.name());
        if (request.slug() != null && !request.slug().equals(product.getSlug())) {
            if (productRepository.existsBySlug(request.slug())) {
                throw new BadRequestException("Product slug already exists");
            }
            product.setSlug(request.slug());
        }
        if (request.description() != null) product.setDescription(request.description());
        if (request.basePrice() != null) product.setBasePrice(request.basePrice());
        if (request.isActive() != null) product.setIsActive(request.isActive());
        if (request.imagePlaceholder() != null) product.setImagePlaceholder(request.imagePlaceholder());
        if (request.imageGallery() != null) product.setImageGallery(request.imageGallery());
        if (request.categoryIds() != null) {
            List<Category> categories = categoryRepository.findAllById(request.categoryIds());
            product.setCategories(new java.util.HashSet<>(categories));
        }

        ProductInventory inventory = productInventoryRepository.findByProductId(id).orElse(null);
        if (inventory != null) {
            if (request.quantityAvailable() != null) inventory.setQuantityAvailable(request.quantityAvailable());
            if (request.lowStockThreshold() != null) inventory.setLowStockThreshold(request.lowStockThreshold());
            productInventoryRepository.save(inventory);
        }

        Product savedProduct = productRepository.save(product);
        return toDetailDto(savedProduct);
    }

    @Transactional
    public void deleteProduct(UUID id) {
        if (!productRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found");
        }
        productRepository.deleteById(id);
    }

    @Transactional
    public void bulkDeleteProducts(List<UUID> ids) {
        productRepository.deleteAllById(ids);
    }

    @Transactional
    public void bulkActivateProducts(List<UUID> ids) {
        List<Product> products = productRepository.findAllById(ids);
        products.forEach(p -> p.setIsActive(true));
        productRepository.saveAll(products);
    }

    @Transactional
    public void bulkDeactivateProducts(List<UUID> ids) {
        List<Product> products = productRepository.findAllById(ids);
        products.forEach(p -> p.setIsActive(false));
        productRepository.saveAll(products);
    }

    private AdminProductListDto toListDto(Product product) {
        Integer qty = product.getInventory() != null ? product.getInventory().getQuantityAvailable() : 0;
        Integer threshold = product.getInventory() != null ? product.getInventory().getLowStockThreshold() : 5;
        List<String> categories = product.getCategories().stream().map(Category::getName).toList();
        return new AdminProductListDto(
                product.getId(), product.getSku(), product.getName(), product.getSlug(),
                product.getBasePrice(), product.getIsActive(), product.getRating(),
                product.getReviewCount(), product.getImagePlaceholder(), qty, threshold,
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
                product.getRating(), product.getReviewCount(), product.getImagePlaceholder(),
                List.copyOf(product.getImageGallery()), categories, inventory,
                product.getCreatedAt(), product.getUpdatedAt()
        );
    }
}
