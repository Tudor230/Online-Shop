import { CommonModule } from '@angular/common';
import { Component, HostListener, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { Background3dComponent } from '../../shared/background-3d/background-3d';
import { LandingHeaderComponent } from '../../shared/landing-header/landing-header';

export interface ValueProp {
  readonly title: string;
  readonly description: string;
}

@Component({
  selector: 'app-welcome',
  standalone: true,
  imports: [CommonModule, RouterLink, Background3dComponent, LandingHeaderComponent],
  templateUrl: './welcome.html'
})
export class WelcomeComponent {
  private readonly router = inject(Router);

  readonly isLoading = signal<boolean>(true);

  readonly introOpacity = signal<number>(1);
  readonly introBlur = signal<number>(0);
  readonly introScale = signal<number>(1);

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

  @HostListener('window:scroll')
  onWindowScroll(): void {
    const scrollY = window.scrollY;
    const dissolvePx = window.innerHeight * 0.85;
    const progress = Math.min(Math.max(scrollY / dissolvePx, 0), 1);
    this.introOpacity.set(1 - progress);
    this.introBlur.set(progress * 8);
    this.introScale.set(1 + progress * 0.03);
  }

  onModelsLoaded(): void {
    setTimeout(() => {
      this.isLoading.set(false);
    }, 500);
  }

  openProductDetails(productId: string): void {
    void this.router.navigate(['/product', productId]);
  }

  addProductToCart(_productId: string): void {}

  saveProductToWishlist(_productId: string): void {}
}
