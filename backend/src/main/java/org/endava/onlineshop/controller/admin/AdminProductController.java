package org.endava.onlineshop.controller.admin;

import org.endava.onlineshop.model.dto.admin.*;
import org.endava.onlineshop.service.admin.AdminProductService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

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
    public AdminProductDetailDto createProduct(@RequestBody AdminProductCreateRequestDto request) {
        return adminProductService.createProduct(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public AdminProductDetailDto updateProduct(@PathVariable UUID id, @RequestBody AdminProductUpdateRequestDto request) {
        return adminProductService.updateProduct(id, request);
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
    public void bulkDeleteProducts(@RequestBody AdminBulkActionRequestDto request) {
        adminProductService.bulkDeleteProducts(request.ids());
    }

    @PostMapping("/bulk-activate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void bulkActivateProducts(@RequestBody AdminBulkActionRequestDto request) {
        adminProductService.bulkActivateProducts(request.ids());
    }

    @PostMapping("/bulk-deactivate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void bulkDeactivateProducts(@RequestBody AdminBulkActionRequestDto request) {
        adminProductService.bulkDeactivateProducts(request.ids());
    }
}
