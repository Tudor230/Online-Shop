import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AdminApiService } from '../../../core/admin/admin-api.service';
import { AdminCategory, AdminProductDetail, AdminUploadedProductImage } from '../../../core/admin/admin.types';
import { CloudinaryTransformOptions } from '../../../core/images/cloudinary-url.pipe';
import { CloudinaryImageFrameComponent } from '../../../shared/cloudinary-image-frame/cloudinary-image-frame';

@Component({
  selector: 'app-admin-product-form',
  standalone: true,
  imports: [CommonModule, FormsModule, CloudinaryImageFrameComponent],
  templateUrl: './product-form.html'
})
export class AdminProductFormComponent implements OnInit {
  private readonly api = inject(AdminApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly productId = signal<string | null>(null);
  readonly categories = signal<AdminCategory[]>([]);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly uploadingImages = signal(false);
  readonly error = signal<string | null>(null);
  readonly imageUploadError = signal<string | null>(null);

  readonly sku = signal('');
  readonly name = signal('');
  readonly slug = signal('');
  readonly description = signal('');
  readonly basePrice = signal(0);
  readonly imagePlaceholder = signal('');
  readonly images = signal<AdminUploadedProductImage[]>([]);
  readonly selectedCategoryIds = signal<string[]>([]);
  readonly initialQuantity = signal(0);
  readonly lowStockThreshold = signal(5);
  readonly isActive = signal(true);
  readonly primaryImageId = computed(() => this.imagePlaceholder());
  readonly imagePreviewOptions: CloudinaryTransformOptions = {
    width: 320,
    height: 240,
    crop: 'pad'
  };

  ngOnInit(): void {
    this.loadCategories();
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.productId.set(id);
      this.loadProduct(id);
    }
  }

  private loadCategories(): void {
    this.api.getCategories().subscribe({
      next: (cats) => this.categories.set(cats),
      error: () => {}
    });
  }

  private loadProduct(id: string): void {
    this.loading.set(true);
    this.api.getProduct(id).subscribe({
      next: (product: AdminProductDetail) => {
        this.sku.set(product.sku);
        this.name.set(product.name);
        this.slug.set(product.slug);
        this.description.set(product.description);
        this.basePrice.set(product.basePrice);
        this.setImages(
          product.imageGallery.map((imageId) => ({ imageId, imageUrl: null, originalFilename: null })),
          product.imagePlaceholder
        );
        this.selectedCategoryIds.set(product.categories.map(c => c.id));
        this.isActive.set(product.isActive);
        if (product.inventory) {
          this.initialQuantity.set(product.inventory.quantityAvailable);
          this.lowStockThreshold.set(product.inventory.lowStockThreshold);
        }
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(err?.message ?? 'Failed to load product');
        this.loading.set(false);
      }
    });
  }

  toggleCategory(id: string): void {
    const current = this.selectedCategoryIds();
    if (current.includes(id)) {
      this.selectedCategoryIds.set(current.filter(c => c !== id));
    } else {
      this.selectedCategoryIds.set([...current, id]);
    }
  }

  onImagesSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const files = Array.from(input.files ?? []);
    if (!files.length) {
      return;
    }

    this.uploadingImages.set(true);
    this.imageUploadError.set(null);

    this.api.uploadProductImages(files).subscribe({
      next: (uploadedImages) => {
        this.setImages(
          [...this.images(), ...uploadedImages],
          this.primaryImageId() || uploadedImages[0]?.imageId || null
        );
        this.uploadingImages.set(false);
        input.value = '';
      },
      error: (err) => {
        this.imageUploadError.set(err?.message ?? 'Failed to upload product images');
        this.uploadingImages.set(false);
        input.value = '';
      }
    });
  }

  setPrimaryImage(imageId: string): void {
    this.setImages(this.images(), imageId);
  }

  removeImage(imageId: string): void {
    const remainingImages = this.images().filter((image) => image.imageId !== imageId);
    const nextPrimaryImageId = this.primaryImageId() === imageId
      ? remainingImages[0]?.imageId ?? null
      : this.primaryImageId();

    this.setImages(remainingImages, nextPrimaryImageId);
  }

  save(): void {
    const validationError = this.validate();
    if (validationError) {
      this.error.set(validationError);
      return;
    }

    this.saving.set(true);
    this.error.set(null);
    const gallery = this.images().map((image) => image.imageId);

    const id = this.productId();
    if (id) {
      this.api.updateProduct(id, {
        sku: this.sku(),
        name: this.name(),
        slug: this.slug(),
        description: this.description() || undefined,
        basePrice: this.basePrice(),
        categoryIds: this.selectedCategoryIds(),
        imagePlaceholder: this.imagePlaceholder(),
        imageGallery: gallery,
        isActive: this.isActive(),
        quantityAvailable: this.initialQuantity(),
        lowStockThreshold: this.lowStockThreshold(),
      }).subscribe({
        next: () => this.router.navigate(['/admin/products']),
        error: (err) => {
          this.error.set(err?.message ?? 'Failed to update product');
          this.saving.set(false);
        }
      });
    } else {
      this.api.createProduct({
        sku: this.sku(),
        name: this.name(),
        slug: this.slug(),
        description: this.description() || undefined,
        basePrice: this.basePrice(),
        categoryIds: this.selectedCategoryIds(),
        imagePlaceholder: this.imagePlaceholder(),
        imageGallery: gallery,
        initialQuantity: this.initialQuantity(),
        lowStockThreshold: this.lowStockThreshold(),
      }).subscribe({
        next: () => this.router.navigate(['/admin/products']),
        error: (err) => {
          this.error.set(err?.message ?? 'Failed to create product');
          this.saving.set(false);
        }
      });
    }
  }

  cancel(): void {
    this.router.navigate(['/admin/products']);
  }

  private validate(): string | null {
    if (!this.sku().trim()) return 'SKU is required';
    if (!this.name().trim()) return 'Name is required';
    if (!this.slug().trim()) return 'Slug is required';
    if (this.basePrice() < 0) return 'Base price cannot be negative';
    if (!this.imagePlaceholder().trim() || this.images().length === 0) return 'At least one product image is required';
    return null;
  }

  private setImages(images: AdminUploadedProductImage[], primaryImageId: string | null | undefined): void {
    const normalizedImages = this.normalizeImages(images, primaryImageId);
    this.images.set(normalizedImages);
    this.imagePlaceholder.set(normalizedImages[0]?.imageId ?? '');
  }

  private normalizeImages(images: AdminUploadedProductImage[], primaryImageId: string | null | undefined): AdminUploadedProductImage[] {
    const uniqueImages = new Map<string, AdminUploadedProductImage>();

    images.forEach((image) => {
      const imageId = image.imageId.trim();
      if (!imageId || uniqueImages.has(imageId)) {
        return;
      }

      uniqueImages.set(imageId, {
        ...image,
        imageId,
      });
    });

    const normalizedPrimary = primaryImageId?.trim() || null;
    const orderedImageIds: string[] = [];

    if (normalizedPrimary && uniqueImages.has(normalizedPrimary)) {
      orderedImageIds.push(normalizedPrimary);
    }

    uniqueImages.forEach((_, imageId) => {
      if (imageId !== normalizedPrimary) {
        orderedImageIds.push(imageId);
      }
    });

    return orderedImageIds.map((imageId) => uniqueImages.get(imageId)!);
  }
}
