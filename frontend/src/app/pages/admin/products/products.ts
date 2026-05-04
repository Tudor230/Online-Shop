import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminApiService } from '../../../core/admin/admin-api.service';
import { AdminProductList, PageResponse } from '../../../core/admin/admin.types';

@Component({
  selector: 'app-admin-products',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './products.html'
})
export class AdminProductsComponent implements OnInit {
  private readonly api = inject(AdminApiService);

  readonly products = signal<AdminProductList[]>([]);
  readonly page = signal(0);
  readonly totalPages = signal(0);
  readonly loading = signal(true);
  readonly selectedIds = signal<Set<string>>(new Set());

  ngOnInit(): void {
    this.loadProducts();
  }

  loadProducts(): void {
    this.loading.set(true);
    this.api.getProducts(this.page()).subscribe({
      next: (res: PageResponse<AdminProductList>) => {
        this.products.set(res.content);
        this.totalPages.set(res.totalPages);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
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
      this.selectedIds.set(new Set(this.products().map(p => p.id)));
    } else {
      this.selectedIds.set(new Set());
    }
  }

  bulkActivate(): void {
    const ids = Array.from(this.selectedIds());
    if (!ids.length) return;
    this.api.bulkActivateProducts({ ids }).subscribe({ next: () => { this.selectedIds.set(new Set()); this.loadProducts(); } });
  }

  bulkDeactivate(): void {
    const ids = Array.from(this.selectedIds());
    if (!ids.length) return;
    this.api.bulkDeactivateProducts({ ids }).subscribe({ next: () => { this.selectedIds.set(new Set()); this.loadProducts(); } });
  }

  bulkDelete(): void {
    const ids = Array.from(this.selectedIds());
    if (!ids.length || !confirm('Delete selected products?')) return;
    this.api.bulkDeleteProducts({ ids }).subscribe({ next: () => { this.selectedIds.set(new Set()); this.loadProducts(); } });
  }
}
