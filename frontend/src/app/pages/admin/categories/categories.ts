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
  readonly error = signal<string | null>(null);
  readonly actionError = signal<string | null>(null);
  readonly editing = signal<AdminCategory | null>(null);
  readonly name = signal('');
  readonly slug = signal('');
  readonly parentId = signal('');

  ngOnInit(): void {
    this.loadCategories();
  }

  loadCategories(): void {
    this.loading.set(true);
    this.error.set(null);
    this.actionError.set(null);
    this.api.getCategories().subscribe({
      next: (data) => {
        this.categories.set(data);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(err?.message ?? 'Failed to load categories');
        this.loading.set(false);
      }
    });
  }

  startCreate(): void {
    this.editing.set({ id: '', parentId: null, name: '', slug: '' });
    this.name.set('');
    this.slug.set('');
    this.parentId.set('');
    this.actionError.set(null);
  }

  startEdit(cat: AdminCategory): void {
    this.editing.set(cat);
    this.name.set(cat.name);
    this.slug.set(cat.slug);
    this.parentId.set(cat.parentId ?? '');
    this.actionError.set(null);
  }

  cancelEdit(): void {
    this.editing.set(null);
    this.actionError.set(null);
  }

  save(): void {
    const editing = this.editing();
    if (!editing) return;

    const nameVal = this.name().trim();
    const slugVal = this.slug().trim();

    if (!nameVal) {
      this.actionError.set('Name is required');
      return;
    }
    if (!slugVal) {
      this.actionError.set('Slug is required');
      return;
    }

    const request = {
      name: nameVal,
      slug: slugVal,
      parentId: this.parentId() || undefined
    };

    this.actionError.set(null);
    if (editing.id) {
      this.api.updateCategory(editing.id, request).subscribe({
        next: () => { this.cancelEdit(); this.loadCategories(); },
        error: (err) => this.actionError.set(err?.message ?? 'Failed to update category')
      });
    } else {
      this.api.createCategory(request).subscribe({
        next: () => { this.cancelEdit(); this.loadCategories(); },
        error: (err) => this.actionError.set(err?.message ?? 'Failed to create category')
      });
    }
  }

  deleteCategory(id: string): void {
    if (!confirm('Delete this category?')) return;
    this.actionError.set(null);
    this.api.deleteCategory(id).subscribe({
      next: () => this.loadCategories(),
      error: (err) => this.actionError.set(err?.message ?? 'Failed to delete category')
    });
  }

  parentOptions(): AdminCategory[] {
    const editing = this.editing();
    return this.categories().filter(c => c.id !== editing?.id);
  }

  parentName(parentId: string | null): string {
    if (!parentId) return '-';
    const parent = this.categories().find(c => c.id === parentId);
    return parent?.name ?? parentId;
  }
}
