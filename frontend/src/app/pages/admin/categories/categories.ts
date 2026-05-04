import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminApiService } from '../../../core/admin/admin-api.service';
import { AdminCategory } from '../../../core/admin/admin.types';

@Component({
  selector: 'app-admin-categories',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './categories.html'
})
export class AdminCategoriesComponent implements OnInit {
  private readonly api = inject(AdminApiService);

  readonly categories = signal<AdminCategory[]>([]);
  readonly loading = signal(true);
  readonly editing = signal<AdminCategory | null>(null);
  readonly name = signal('');
  readonly slug = signal('');
  readonly parentId = signal('');

  ngOnInit(): void {
    this.loadCategories();
  }

  loadCategories(): void {
    this.loading.set(true);
    this.api.getCategories().subscribe({
      next: (data) => {
        this.categories.set(data);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  startCreate(): void {
    this.editing.set({ id: '', parentId: null, name: '', slug: '' });
    this.name.set('');
    this.slug.set('');
    this.parentId.set('');
  }

  startEdit(cat: AdminCategory): void {
    this.editing.set(cat);
    this.name.set(cat.name);
    this.slug.set(cat.slug);
    this.parentId.set(cat.parentId ?? '');
  }

  cancelEdit(): void {
    this.editing.set(null);
  }

  save(): void {
    const editing = this.editing();
    if (!editing) return;

    const request = {
      name: this.name(),
      slug: this.slug(),
      parentId: this.parentId() || undefined
    };

    if (editing.id) {
      this.api.updateCategory(editing.id, request).subscribe({ next: () => { this.cancelEdit(); this.loadCategories(); } });
    } else {
      this.api.createCategory(request).subscribe({ next: () => { this.cancelEdit(); this.loadCategories(); } });
    }
  }

  deleteCategory(id: string): void {
    if (!confirm('Delete this category?')) return;
    this.api.deleteCategory(id).subscribe({ next: () => this.loadCategories() });
  }
}
