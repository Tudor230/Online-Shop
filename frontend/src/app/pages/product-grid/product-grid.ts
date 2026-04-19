import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { mockProducts, type Product } from '../../../assets/data/mock-products';
import { ProductCardComponent } from '../../shared/ui/product-card/product-card';

@Component({
  selector: 'app-product-grid',
  standalone: true,
  imports: [CommonModule, ProductCardComponent],
  templateUrl: './product-grid.html'
})
export class ProductGridComponent {
  private readonly router = inject(Router);

  readonly isLoading = signal(false);
  readonly products = signal<readonly Product[]>(mockProducts);
  readonly skeletonItems = Array.from({ length: 8 }, (_, index) => index);

  openProductDetails(productId: string): void {
    void this.router.navigate(['/product', productId]);
  }

  addProductToCart(_productId: string): void {}

  saveProductToWishlist(_productId: string): void {}
}
