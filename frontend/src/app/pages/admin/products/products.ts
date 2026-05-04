import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AdminApiService } from '../../../core/admin/admin-api.service';
import { AdminProductList, PageResponse } from '../../../core/admin/admin.types';

@Component({
  selector: 'app-admin-products',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './products.html'
})
export class AdminProductsComponent implements OnInit {
  private readonly api = inject(AdminApiService);

  readonly products = signal<AdminProductList[]>([]);
  readonly page = signal(0);
  readonly totalPages = signal(0);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly actionError = signal<string | null>(null);
  readonly selectedIds = signal<Set<string>>(new Set());
  readonly searchQuery = signal('');

  ngOnInit(): void {
    this.loadProducts();
  }

  loadProducts(): void {
    this.loading.set(true);
    this.error.set(null);
    this.actionError.set(null);
    this.api.getProducts(this.page()).subscribe({
      next: (res: PageResponse<AdminProductList>) => {
        this.products.set(res.content);
        this.totalPages.set(res.totalPages);
        this.loading.set(false);
        this.selectedIds.set(new Set());
      },
      error: (err) => {
        this.error.set(err?.message ?? 'Failed to load products');
        this.loading.set(false);
      }
    });
  }

  prevPage(): void {
    if (this.page() > 0) {
      this.page.set(this.page() - 1);
      this.loadProducts();
    }
  }

  nextPage(): void {
    if (this.page() < this.totalPages() - 1) {
      this.page.set(this.page() + 1);
      this.loadProducts();
    }
  }

  toggleSelection(id: string): void {
    const set = new Set(this.selectedIds());
    if (set.has(id)) {
      set.delete(id);
    } else {
      set.add(id);
    }
    this.selectedIds.set(set);
  }

  selectAll(event: Event): void {
    const checked = (event.target as HTMLInputElement).checked;
    if (checked) {
      this.selectedIds.set(new Set(this.filteredProducts().map(p => p.id)));
    } else {
      this.selectedIds.set(new Set());
    }
  }

  bulkActivate(): void {
    const ids = Array.from(this.selectedIds());
    if (!ids.length) return;
    this.actionError.set(null);
    this.api.bulkActivateProducts({ ids }).subscribe({
      next: () => { this.selectedIds.set(new Set()); this.loadProducts(); },
      error: (err) => this.actionError.set(err?.message ?? 'Failed to activate products')
    });
  }

  bulkDeactivate(): void {
    const ids = Array.from(this.selectedIds());
    if (!ids.length) return;
    this.actionError.set(null);
    this.api.bulkDeactivateProducts({ ids }).subscribe({
      next: () => { this.selectedIds.set(new Set()); this.loadProducts(); },
      error: (err) => this.actionError.set(err?.message ?? 'Failed to deactivate products')
    });
  }

  bulkDelete(): void {
    const ids = Array.from(this.selectedIds());
    if (!ids.length || !confirm('Delete selected products?')) return;
    this.actionError.set(null);
    this.api.bulkDeleteProducts({ ids }).subscribe({
      next: () => { this.selectedIds.set(new Set()); this.loadProducts(); },
      error: (err) => this.actionError.set(err?.message ?? 'Failed to delete products')
    });
  }

  filteredProducts(): AdminProductList[] {
    const q = this.searchQuery().toLowerCase().trim();
    if (!q) return this.products();
    return this.products().filter(p =>
      p.name.toLowerCase().includes(q) ||
      p.sku.toLowerCase().includes(q)
    );
  }
}
