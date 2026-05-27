package org.endava.onlineshop.service.admin;

import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.endava.onlineshop.exception.BadRequestException;
import org.endava.onlineshop.model.dto.admin.AdminUploadedProductImageDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

@Service
public class ProductImageStorageService {

    private static final String CLOUDINARY_ADMIN_FOLDER = "online-shop/products/admin";

    private final String cloudinaryCloudName;
    private final String cloudinaryApiKey;
    private final String cloudinaryApiSecret;

    public ProductImageStorageService(
            @Value("${cloudinary.cloud-name:}") String cloudinaryCloudName,
            @Value("${cloudinary.api-key:}") String cloudinaryApiKey,
            @Value("${cloudinary.api-secret:}") String cloudinaryApiSecret
    ) {
        this.cloudinaryCloudName = cloudinaryCloudName;
        this.cloudinaryApiKey = cloudinaryApiKey;
        this.cloudinaryApiSecret = cloudinaryApiSecret;
    }

    public List<AdminUploadedProductImageDto> uploadProductImages(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new BadRequestException("At least one image file is required");
        }
        if (!isCloudinaryConfigured()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Product image uploads are not configured");
        }

        Cloudinary cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudinaryCloudName,
                "api_key", cloudinaryApiKey,
                "api_secret", cloudinaryApiSecret,
                "secure", true
        ));

        List<AdminUploadedProductImageDto> uploadedImages = new ArrayList<>();
        for (MultipartFile file : files) {
            uploadedImages.add(uploadImage(cloudinary, file));
        }
        return uploadedImages;
    }

    private AdminUploadedProductImageDto uploadImage(Cloudinary cloudinary, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Uploaded image files cannot be empty");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BadRequestException("Only image files are allowed");
        }

        String originalFilename = file.getOriginalFilename() != null && !file.getOriginalFilename().isBlank()
                ? file.getOriginalFilename()
                : "product-image";
        String generatedPublicId = sanitizeFileName(originalFilename) + "-" + UUID.randomUUID();

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "folder", CLOUDINARY_ADMIN_FOLDER,
                    "public_id", generatedPublicId,
                    "overwrite", false,
                    "resource_type", "image"
            ));

            Object publicId = uploadResult.get("public_id");
            if (!(publicId instanceof String imageId) || imageId.isBlank()) {
                throw new IllegalStateException("Cloudinary did not return an image identifier");
            }

            Object secureUrl = uploadResult.get("secure_url");
            return new AdminUploadedProductImageDto(
                    imageId,
                    secureUrl instanceof String url ? url : "",
                    originalFilename
            );
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read uploaded image " + originalFilename, ex);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to upload product image " + originalFilename, ex);
        }
    }

    private boolean isCloudinaryConfigured() {
        return cloudinaryCloudName != null && !cloudinaryCloudName.isBlank()
                && cloudinaryApiKey != null && !cloudinaryApiKey.isBlank()
                && cloudinaryApiSecret != null && !cloudinaryApiSecret.isBlank();
    }

    private String sanitizeFileName(String fileName) {
        String withoutExtension = stripExtension(fileName);
        String normalized = Normalizer.normalize(withoutExtension, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase();

        String slug = normalized
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");

        return slug.isBlank() ? "product-image" : slug;
    }

    private String stripExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex <= 0) {
            return fileName;
        }
        return fileName.substring(0, dotIndex);
    }
}
