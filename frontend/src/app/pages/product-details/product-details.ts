import { Component, computed, effect, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { map } from 'rxjs';
import { mockProducts } from '../../../assets/data/mock-products';
import { ProductDisplayComponent } from '../../shared/product-display/product-display';

@Component({
  selector: 'app-product-details',
  standalone: true,
  imports: [ProductDisplayComponent],
  templateUrl: './product-details.html'
})
export class ProductDetailsComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly isLoading = signal(false);
  readonly selectedImageIndex = signal(0);
  private readonly productId = toSignal(this.route.paramMap.pipe(map((params) => params.get('id'))), {
    initialValue: null
  });
  readonly product = computed(() => mockProducts.find((item) => item.id === this.productId()) ?? null);
  readonly selectedImage = computed(() => {
    const currentProduct = this.product();
    if (!currentProduct) {
      return '';
    }
    return (
      currentProduct.imageGallery[this.selectedImageIndex()] ??
      currentProduct.imageGallery[0] ??
      currentProduct.imagePlaceholder
    );
  });

  constructor() {
    effect(() => {
      this.productId();
      this.selectedImageIndex.set(0);
    });
  }

  selectImage(index: number): void {
    this.selectedImageIndex.set(index);
  }

  goBackToGrid(): void {
    void this.router.navigateByUrl('/');
  }

  addToCart(): void {}

  saveToWishlist(): void {}
}
