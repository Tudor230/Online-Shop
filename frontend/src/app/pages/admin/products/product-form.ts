import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AdminApiService } from '../../../core/admin/admin-api.service';
import { AdminCategory, AdminProductDetail } from '../../../core/admin/admin.types';

@Component({
  selector: 'app-admin-product-form',
  standalone: true,
  imports: [CommonModule, FormsModule],
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
  readonly error = signal<string | null>(null);

  readonly sku = signal('');
  readonly name = signal('');
  readonly slug = signal('');
  readonly description = signal('');
  readonly basePrice = signal(0);
  readonly imagePlaceholder = signal('');
  readonly imageGallery = signal('');
  readonly selectedCategoryIds = signal<string[]>([]);
  readonly initialQuantity = signal(0);
  readonly lowStockThreshold = signal(5);
  readonly isActive = signal(true);

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
        this.imagePlaceholder.set(product.imagePlaceholder);
        this.imageGallery.set(product.imageGallery.join('\n'));
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

  save(): void {
    const validationError = this.validate();
    if (validationError) {
      this.error.set(validationError);
      return;
    }

    this.saving.set(true);
    this.error.set(null);

    const gallery = this.imageGallery()
      .split('\n')
      .map(s => s.trim())
      .filter(s => s.length > 0);

    const request = {
      sku: this.sku(),
      name: this.name(),
      slug: this.slug(),
      description: this.description() || undefined,
      basePrice: this.basePrice(),
      categoryIds: this.selectedCategoryIds(),
      imagePlaceholder: this.imagePlaceholder(),
      imageGallery: gallery.length > 0 ? gallery : undefined,
      initialQuantity: this.initialQuantity(),
      lowStockThreshold: this.lowStockThreshold(),
    };

    const id = this.productId();
    if (id) {
      this.api.updateProduct(id, {
        ...request,
        isActive: this.isActive(),
        quantityAvailable: this.initialQuantity(),
      }).subscribe({
        next: () => this.router.navigate(['/admin/products']),
        error: (err) => {
          this.error.set(err?.message ?? 'Failed to update product');
          this.saving.set(false);
        }
      });
    } else {
      this.api.createProduct(request).subscribe({
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
    if (!this.imagePlaceholder().trim()) return 'Image placeholder is required';
    return null;
  }
}
