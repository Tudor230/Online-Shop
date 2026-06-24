package org.endava.onlineshop.controller.admin;

import java.util.List;
import java.util.UUID;

import org.endava.onlineshop.model.dto.admin.AdminBulkActionRequestDto;
import org.endava.onlineshop.model.dto.admin.AdminProductCreateRequestDto;
import org.endava.onlineshop.model.dto.admin.AdminProductDetailDto;
import org.endava.onlineshop.model.dto.admin.AdminProductListDto;
import org.endava.onlineshop.model.dto.admin.AdminProductUpdateRequestDto;
import org.endava.onlineshop.model.dto.admin.AdminUploadedProductImageDto;
import org.endava.onlineshop.service.admin.AdminProductService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/products")
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPPORT')")
public class AdminProductController {

    private final AdminProductService adminProductService;

    public AdminProductController(AdminProductService adminProductService) {
        this.adminProductService = adminProductService;
    }

    @GetMapping
    public Page<AdminProductListDto> getProducts(Pageable pageable) {
        return adminProductService.getProducts(pageable);
    }

    @GetMapping("/{id}")
    public AdminProductDetailDto getProduct(@PathVariable UUID id) {
        return adminProductService.getProduct(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public AdminProductDetailDto createProduct(@Valid @RequestBody AdminProductCreateRequestDto request) {
        return adminProductService.createProduct(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public AdminProductDetailDto updateProduct(@PathVariable UUID id, @Valid @RequestBody AdminProductUpdateRequestDto request) {
        return adminProductService.updateProduct(id, request);
    }

    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public List<AdminUploadedProductImageDto> uploadProductImages(@RequestParam("files") List<MultipartFile> files) {
        return adminProductService.uploadProductImages(files);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteProduct(@PathVariable UUID id) {
        adminProductService.deleteProduct(id);
    }

    @PostMapping("/bulk-delete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void bulkDeleteProducts(@Valid @RequestBody AdminBulkActionRequestDto request) {
        adminProductService.bulkDeleteProducts(request.ids());
    }

    @PostMapping("/bulk-activate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void bulkActivateProducts(@Valid @RequestBody AdminBulkActionRequestDto request) {
        adminProductService.bulkActivateProducts(request.ids());
    }

    @PostMapping("/bulk-deactivate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void bulkDeactivateProducts(@Valid @RequestBody AdminBulkActionRequestDto request) {
        adminProductService.bulkDeactivateProducts(request.ids());
    }
}
