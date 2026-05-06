package org.endava.onlineshop.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.endava.onlineshop.model.entities.Category;
import org.endava.onlineshop.model.entities.Product;
import org.endava.onlineshop.model.entities.ProductInventory;
import org.endava.onlineshop.repository.CategoryRepository;
import org.endava.onlineshop.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

// TODO: Remove before deploying to production.
@Component
@Profile("dev")
public class ProductCatalogSeeder implements ApplicationRunner {

    private static final String CLOUDINARY_SEED_FOLDER = "online-shop/products/seed";
    private static final Logger LOGGER = LoggerFactory.getLogger(ProductCatalogSeeder.class);

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ObjectMapper objectMapper;
    private final Resource seedResource;
    private final ResourceLoader resourceLoader;
    private final String cloudinaryCloudName;
    private final String cloudinaryApiKey;
    private final String cloudinaryApiSecret;
    private final ProductEmbeddingService productEmbeddingService;

    public ProductCatalogSeeder(
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            ObjectMapper objectMapper,
            @Value("classpath:seed/mock-products.json") Resource seedResource,
            ResourceLoader resourceLoader,
            @Value("${cloudinary.cloud-name:}") String cloudinaryCloudName,
            @Value("${cloudinary.api-key:}") String cloudinaryApiKey,
            @Value("${cloudinary.api-secret:}") String cloudinaryApiSecret,
            ProductEmbeddingService productEmbeddingService
    ) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.objectMapper = objectMapper;
        this.seedResource = seedResource;
        this.resourceLoader = resourceLoader;
        this.cloudinaryCloudName = cloudinaryCloudName;
        this.cloudinaryApiKey = cloudinaryApiKey;
        this.cloudinaryApiSecret = cloudinaryApiSecret;
        this.productEmbeddingService = productEmbeddingService;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!isCloudinaryConfigured()) {
            LOGGER.warn("Skipping product image seeding because Cloudinary credentials are not fully configured.");
            return;
        }

        List<SeedProduct> seedProducts = loadSeedProducts();
        Map<String, String> uploadedImageIdsByName = uploadSeedImages(seedProducts);
        seedProducts.forEach(seedProduct -> seedProduct(seedProduct, uploadedImageIdsByName));
    }

    private boolean isCloudinaryConfigured() {
        return cloudinaryCloudName != null && !cloudinaryCloudName.isBlank()
                && cloudinaryApiKey != null && !cloudinaryApiKey.isBlank()
                && cloudinaryApiSecret != null && !cloudinaryApiSecret.isBlank();
    }

    private void seedProduct(SeedProduct seedProduct, Map<String, String> uploadedImageIdsByName) {
        String slug = seedProduct.id();
        List<Category> categoryHierarchy = resolveOrCreateCategoryHierarchy(seedProduct.category());

        Product product = productRepository.findBySlug(slug).orElseGet(Product::new);
        product.setSlug(slug);
        product.setName(seedProduct.title());
        product.setSku(toSku(slug));
        product.setBasePrice(BigDecimal.valueOf(seedProduct.price()));
        product.setDescription(seedProduct.description());
        product.setIsActive(true);
        product.setRating(seedProduct.rating());
        product.setReviewCount(seedProduct.reviewCount());

        String primaryImageId = uploadedImageIdsByName.get(seedProduct.imageName());
        if (primaryImageId == null || primaryImageId.isBlank()) {
            throw new IllegalStateException("Missing uploaded Cloudinary image for " + seedProduct.imageName());
        }
        product.setImageId(primaryImageId);

        List<String> galleryImageIds = seedProduct.imageGalleryNames().stream()
                .map(uploadedImageIdsByName::get)
                .filter(imageId -> imageId != null && !imageId.isBlank())
                .toList();
        product.setImageGalleryIds(new ArrayList<>(galleryImageIds));

        Set<Category> categories = product.getCategories();
        categories.clear();
        categories.addAll(categoryHierarchy);
        product.setCategoryText(productEmbeddingService.buildCategoryText(categories));

        ProductInventory inventory = product.getInventory();
        if (inventory == null) {
            inventory = new ProductInventory();
        }
        inventory.setQuantityAvailable(25);
        inventory.setLowStockThreshold(5);
        product.setInventory(inventory);

        Product savedProduct = productRepository.saveAndFlush(product);
        productEmbeddingService.upsertProductEmbedding(savedProduct);
    }

    private Map<String, String> uploadSeedImages(List<SeedProduct> seedProducts) {
        Cloudinary cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudinaryCloudName,
                "api_key", cloudinaryApiKey,
                "api_secret", cloudinaryApiSecret,
                "secure", true
        ));

        Set<String> uniqueImageNames = new LinkedHashSet<>();
        for (SeedProduct seedProduct : seedProducts) {
            uniqueImageNames.add(seedProduct.imageName());
            uniqueImageNames.addAll(seedProduct.imageGalleryNames());
        }

        Map<String, String> uploadedByName = new LinkedHashMap<>();
        for (String imageName : uniqueImageNames) {
            uploadedByName.put(imageName, uploadImage(cloudinary, imageName));
        }

        return uploadedByName;
    }

    private String uploadImage(Cloudinary cloudinary, String imageName) {
        Resource imageResource = resourceLoader.getResource("classpath:seed/images/" + imageName);
        if (!imageResource.exists()) {
            throw new IllegalStateException("Seed image not found: classpath:seed/images/" + imageName);
        }

        String basePublicId = stripExtension(imageName);
        String fullPublicId = CLOUDINARY_SEED_FOLDER + "/" + basePublicId;

        try (InputStream imageStream = imageResource.getInputStream()) {
            cloudinary.uploader().upload(imageStream.readAllBytes(), ObjectUtils.asMap(
                    "folder", CLOUDINARY_SEED_FOLDER,
                    "public_id", basePublicId,
                    "overwrite", false,
                    "resource_type", "image"
            ));
            return fullPublicId;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to upload seed image to Cloudinary: " + imageName, ex);
        }
    }


    private String stripExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex <= 0) {
            return fileName;
        }
        return fileName.substring(0, dotIndex);
    }

    private List<Category> resolveOrCreateCategoryHierarchy(String categoryHierarchyText) {
        List<String> levels = parseCategoryHierarchy(categoryHierarchyText);
        List<Category> hierarchy = new ArrayList<>();
        UUID parentId = null;
        String parentPath = null;

        for (String level : levels) {
            String normalizedName = level.trim();
            String slug = toSlug(normalizedName);
            String path = appendPath(parentPath, slug);

            Category category = categoryRepository.findByParentIdAndSlug(parentId, slug).orElse(null);
            if (category == null) {
                category = createCategory(normalizedName, slug, parentId, path);
            }

            if (category.getPath() == null || category.getPath().isBlank()) {
                category.setPath(path);
                category = categoryRepository.saveAndFlush(category);
            }

            hierarchy.add(category);
            parentId = category.getId();
            parentPath = category.getPath();
        }

        return hierarchy;
    }

    private List<String> parseCategoryHierarchy(String categoryHierarchyText) {
        List<String> hierarchy = Arrays.stream((categoryHierarchyText == null ? "" : categoryHierarchyText)
                        .replace('/', '>')
                        .split(">"))
                .map(String::trim)
                .filter(level -> !level.isBlank())
                .toList();

        return hierarchy.isEmpty() ? List.of("Uncategorized") : hierarchy;
    }

    private String appendPath(String parentPath, String slug) {
        String node = slug.replace('-', '_');
        if (parentPath == null || parentPath.isBlank()) {
            return node;
        }
        return parentPath + "." + node;
    }

    private Category createCategory(String categoryName, String categorySlug, UUID parentId, String path) {
        Category category = new Category();
        category.setName(categoryName);
        category.setSlug(categorySlug);
        category.setParentId(parentId);
        category.setPath(path);
        return categoryRepository.saveAndFlush(category);
    }

    private List<SeedProduct> loadSeedProducts() {
        try {
            return objectMapper.readValue(seedResource.getInputStream(), new TypeReference<>() {});
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load product seed data", ex);
        }
    }

    private String toSlug(String value) {
        return value.toLowerCase().replace(" ", "-");
    }

    private String toSku(String slug) {
        return "SKU-" + slug.toUpperCase().replace("-", "_");
    }

    private record SeedProduct(
            String id,
            String category,
            String title,
            double rating,
            int reviewCount,
            double price,
            String description,
            String imageName,
            List<String> imageGalleryNames,
            List<SeedColorOption> availableColors
    ) {
    }

    private record SeedColorOption(String name, String swatch) {
    }
}

