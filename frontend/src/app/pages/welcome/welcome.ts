import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { mockProducts, type Product } from '../../../assets/data/mock-products';
import { Hero3dComponent } from '../../shared/hero-3d/hero-3d';
import { ProductCardComponent } from '../../shared/product-card/product-card';

export interface ValueProp {
  readonly title: string;
  readonly description: string;
}

@Component({
  selector: 'app-welcome',
  standalone: true,
  imports: [CommonModule, RouterLink, ProductCardComponent, Hero3dComponent],
  templateUrl: './welcome.html'
})
export class WelcomeComponent {
  private readonly router = inject(Router);

  readonly products = signal<readonly Product[]>(mockProducts);

  readonly valueProps = signal<readonly ValueProp[]>([
    {
      title: 'Free Delivery',
      description: 'Complimentary shipping on orders over $50. Arrives in 2–4 business days with tracking.'
    },
    {
      title: '30-Day Free Returns',
      description: 'Changed your mind? Send it back within 30 days for a full refund—no questions, no hassle.'
    },
    {
      title: 'Secure Checkout',
      description: 'End-to-end encryption keeps your payment details private and protected at every step.'
    }
  ]);

  openProductDetails(productId: string): void {
    void this.router.navigate(['/product', productId]);
  }

  addProductToCart(_productId: string): void {}

  saveProductToWishlist(_productId: string): void {}
}
