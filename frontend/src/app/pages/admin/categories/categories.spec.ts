import { describe, it, expect, vi, beforeEach } from 'vitest';
import { AdminCategoriesComponent } from './categories';
import { AdminApiService } from '../../../core/admin/admin-api.service';
import { of, throwError } from 'rxjs';
import { AdminCategory } from '../../../core/admin/admin.types';
import { createEnvironmentInjector, runInInjectionContext, EnvironmentInjector } from '@angular/core';

const createMockApi = () => ({
  getCategories: vi.fn(),
  createCategory: vi.fn(),
  updateCategory: vi.fn(),
  deleteCategory: vi.fn(),
});

describe('AdminCategoriesComponent', () => {
  let component: AdminCategoriesComponent;
  let mockApi: ReturnType<typeof createMockApi>;
  let injector: EnvironmentInjector;

  beforeEach(() => {
    mockApi = createMockApi();
    injector = createEnvironmentInjector([
      { provide: AdminApiService, useValue: mockApi },
    ], null as any);

    component = runInInjectionContext(injector, () => new AdminCategoriesComponent());
  });

  it('should load categories on init', () => {
    const cats: AdminCategory[] = [
      { id: '1', parentId: null, name: 'Electronics', slug: 'electronics' },
    ];
    mockApi.getCategories.mockReturnValue(of(cats));

    component.ngOnInit();

    expect(component.categories()).toEqual(cats);
    expect(component.loading()).toBe(false);
    expect(component.error()).toBeNull();
  });

  it('should set error when loading fails', () => {
    mockApi.getCategories.mockReturnValue(throwError(() => new Error('Network error')));

    component.ngOnInit();

    expect(component.loading()).toBe(false);
    expect(component.error()).toBe('Network error');
  });

  it('should validate required fields on save', () => {
    component.startCreate();
    component.name.set('   ');
    component.slug.set('slug');

    component.save();

    expect(component.actionError()).toBe('Name is required');
    expect(mockApi.createCategory).not.toHaveBeenCalled();
  });

  it('should validate slug on save', () => {
    component.startCreate();
    component.name.set('Name');
    component.slug.set('   ');

    component.save();

    expect(component.actionError()).toBe('Slug is required');
    expect(mockApi.createCategory).not.toHaveBeenCalled();
  });

  it('should create category when valid', () => {
    mockApi.createCategory.mockReturnValue(of({ id: '2', parentId: null, name: 'Books', slug: 'books' }));
    mockApi.getCategories.mockReturnValue(of([]));

    component.startCreate();
    component.name.set('Books');
    component.slug.set('books');
    component.save();

    expect(mockApi.createCategory).toHaveBeenCalledWith({ name: 'Books', slug: 'books', parentId: undefined });
    expect(component.editing()).toBeNull();
  });

  it('should find parent name by id', () => {
    component.categories.set([
      { id: '1', parentId: null, name: 'Parent', slug: 'parent' },
      { id: '2', parentId: '1', name: 'Child', slug: 'child' },
    ]);

    expect(component.parentName('1')).toBe('Parent');
    expect(component.parentName(null)).toBe('-');
    expect(component.parentName('999')).toBe('999');
  });

  it('should exclude current category from parent options', () => {
    component.categories.set([
      { id: '1', parentId: null, name: 'A', slug: 'a' },
      { id: '2', parentId: null, name: 'B', slug: 'b' },
    ]);
    component.startEdit({ id: '1', parentId: null, name: 'A', slug: 'a' });

    const options = component.parentOptions();
    expect(options.length).toBe(1);
    expect(options[0].id).toBe('2');
  });
});
