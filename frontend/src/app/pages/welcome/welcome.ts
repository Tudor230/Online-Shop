import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { mockProducts, type Product } from '../../../assets/data/mock-products';
import { ProductCardComponent } from '../../shared/product-card/product-card';

@Component({
  selector: 'app-welcome',
  standalone: true,
  imports: [CommonModule, RouterLink, ProductCardComponent],
  templateUrl: './welcome.html'
})
export class WelcomeComponent {
  private readonly router = inject(Router);

  readonly products = signal<readonly Product[]>(mockProducts);

  navigateToProducts(): void {
    void this.router.navigate(['/']);
  }

  openProductDetails(productId: string): void {
    void this.router.navigate(['/product', productId]);
  }

  addProductToCart(_productId: string): void {}

  saveProductToWishlist(_productId: string): void {}
}