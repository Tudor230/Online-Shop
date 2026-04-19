import { CommonModule } from '@angular/common';
import { Component, signal } from '@angular/core';
import { mockProducts, type Product } from '../../../assets/data/mock-products';
import { ProductCardComponent } from '../../ui/product-card/product-card';

@Component({
  selector: 'app-product-grid',
  standalone: true,
  imports: [CommonModule, ProductCardComponent],
  templateUrl: './product-grid.html'
})
export class ProductGridComponent {
  readonly isLoading = signal(false);
  readonly products = signal<readonly Product[]>(mockProducts);
  readonly skeletonItems = Array.from({ length: 8 }, (_, index) => index);
}
