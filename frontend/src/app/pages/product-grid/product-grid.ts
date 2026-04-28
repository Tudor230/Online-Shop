import { CommonModule } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { Router } from '@angular/router';
import { catchError, map, of, startWith } from 'rxjs';
import { CartFacadeService } from '../../core/cart/cart-facade.service';
import { ProductApiService } from '../../core/products/product-api.service';
import { type ProductSummary } from '../../core/products/product.types';
import { ProductCardComponent } from '../../shared/product-card/product-card';

@Component({
  selector: 'app-product-grid',
  standalone: true,
  imports: [CommonModule, ProductCardComponent],
  templateUrl: './product-grid.html'
})
export class ProductGridComponent {
  private readonly router = inject(Router);
  private readonly productApiService = inject(ProductApiService);
  private readonly cartFacadeService = inject(CartFacadeService);

  private readonly productListState = toSignal(
    this.productApiService.getProducts().pipe(
      map((products) => ({ isLoading: false, products })),
      startWith({ isLoading: true, products: [] as ProductSummary[] }),
      catchError(() => of({ isLoading: false, products: [] as ProductSummary[] }))
    ),
    { initialValue: { isLoading: true, products: [] as ProductSummary[] } }
  );

  readonly isLoading = computed(() => this.productListState().isLoading);
  readonly products = computed(() => this.productListState().products);
  readonly skeletonItems = Array.from({ length: 8 }, (_, index) => index);

  openProductDetails(productId: string): void {
    void this.router.navigate(['/product', productId]);
  }

  addProductToCart(productId: string): void {
    this.cartFacadeService.addItem(productId);
  }

  saveProductToWishlist(_productId: string): void {}
}
