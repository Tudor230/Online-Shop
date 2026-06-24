import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CategoryTreeNode } from '../../core/products/product.types';

@Component({
  selector: 'app-category-sidebar',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './category-sidebar.html'
})
export class CategorySidebarComponent {
  @Input({ required: true }) categories: CategoryTreeNode[] = [];
  @Input() activeCategoryPath: string | null = null;
  @Output() categorySelected = new EventEmitter<string | null>();

  collapsed = new Set<string>();

  toggleCollapse(categoryId: string): void {
    if (this.collapsed.has(categoryId)) {
      this.collapsed.delete(categoryId);
    } else {
      this.collapsed.add(categoryId);
    }
  }

  isCollapsed(categoryId: string): boolean {
    return this.collapsed.has(categoryId);
  }

  selectCategory(path: string | null): void {
    this.categorySelected.emit(path);
  }
}
