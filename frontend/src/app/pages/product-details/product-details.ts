import { Component, computed, effect, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { catchError, map, of, startWith, switchMap } from 'rxjs';
import { ProductApiService } from '../../core/products/product-api.service';
import { ProductDetails } from '../../core/products/product.types';
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
  private readonly productApiService = inject(ProductApiService);

  readonly selectedImageIndex = signal(0);

  private readonly productState = toSignal(
    this.route.paramMap.pipe(
      map((params) => params.get('id')),
      switchMap((productId) => {
        if (!productId) {
          return of({ isLoading: false, product: null as ProductDetails | null });
        }

        return this.productApiService.getProductById(productId).pipe(
          map((product) => ({ isLoading: false, product })),
          startWith({ isLoading: true, product: null as ProductDetails | null }),
          catchError(() => of({ isLoading: false, product: null as ProductDetails | null }))
        );
      })
    ),
    { initialValue: { isLoading: true, product: null as ProductDetails | null } }
  );

  readonly isLoading = computed(() => this.productState().isLoading);
  readonly product = computed(() => this.productState().product);
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
      this.product();
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
