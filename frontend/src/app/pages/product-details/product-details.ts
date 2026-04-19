import { CommonModule, CurrencyPipe } from '@angular/common';
import { Component, computed, effect, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { map } from 'rxjs';
import { mockProducts } from '../../../assets/data/mock-products';

@Component({
  selector: 'app-product-details',
  standalone: true,
  imports: [CommonModule, CurrencyPipe, RouterLink],
  templateUrl: './product-details.html'
})
export class ProductDetailsComponent {
  private readonly route = inject(ActivatedRoute);

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
}
