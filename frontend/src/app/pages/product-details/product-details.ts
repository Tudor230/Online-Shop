import { CommonModule, DOCUMENT, isPlatformBrowser } from '@angular/common';
import { Component, ElementRef, PLATFORM_ID, ViewChild, computed, effect, inject, signal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { combineLatest, firstValueFrom, catchError, map, of, startWith, switchMap } from 'rxjs';
import { AuthStateService } from '../../core/auth/auth-state.service';
import { CartFacadeService } from '../../core/cart/cart-facade.service';
import { ProductApiService } from '../../core/products/product-api.service';
import { CreateProductReviewRequest, ProductDetails, ProductSummary } from '../../core/products/product.types';
import { WishlistFacadeService } from '../../core/wishlist/wishlist-facade.service';
import { ProductDisplayComponent } from '../../shared/product-display/product-display';
import { ProductCardComponent } from '../../shared/product-card/product-card';
import { StarRatingComponent } from '../../shared/star-rating/star-rating';

interface SimilarItemsState {
  isLoading: boolean;
  items: ProductSummary[];
}

@Component({
  selector: 'app-product-details',
  standalone: true,
  imports: [CommonModule, ProductDisplayComponent, ProductCardComponent, StarRatingComponent],
  templateUrl: './product-details.html'
})
export class ProductDetailsComponent {
  private static readonly SIMILAR_ITEMS_SIZE = 8;

  @ViewChild('similarItemsTrack') private similarItemsTrack?: ElementRef<HTMLElement>;

  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly productApiService = inject(ProductApiService);
  private readonly cartFacadeService = inject(CartFacadeService);
  private readonly wishlistFacadeService = inject(WishlistFacadeService);
  private readonly authState = inject(AuthStateService);
  private readonly document = inject(DOCUMENT);
  private readonly platformId = inject(PLATFORM_ID);
  private readonly isBrowser = isPlatformBrowser(this.platformId);

  readonly selectedImageIndex = signal(0);
  private readonly reloadToken = signal(0);
  readonly isSubmittingReview = signal(false);
  readonly reviewError = signal<string | null>(null);
  readonly reviewFormResetToken = signal(0);

  private readonly productState = toSignal(
    combineLatest([
      this.route.paramMap.pipe(map((params) => params.get('slug'))),
      toObservable(this.reloadToken)
    ]).pipe(
      switchMap(([productSlug]) => {
        if (!productSlug) {
          return of({ isLoading: false, product: null as ProductDetails | null });
        }

        return this.productApiService.getProductBySlug(productSlug).pipe(
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
  private readonly similarItemsState = toSignal(
    toObservable(this.product).pipe(
      switchMap((currentProduct) => {
        if (!currentProduct) {
          return of({ isLoading: false, items: [] as ProductSummary[] });
        }

        return this.productApiService
          .getSimilarProducts(currentProduct.slug, ProductDetailsComponent.SIMILAR_ITEMS_SIZE)
          .pipe(
            map((items) => ({
              isLoading: false,
              items
            })),
          startWith({ isLoading: true, items: [] as ProductSummary[] }),
          catchError(() => of({ isLoading: false, items: [] as ProductSummary[] }))
          );
      })
    ),
    { initialValue: { isLoading: true, items: [] as ProductSummary[] } }
  );
  readonly similarItems = computed(() => this.similarItemsState().items);
  readonly isSimilarItemsLoading = computed(() => this.similarItemsState().isLoading);
  readonly isAuthenticated = this.authState.isAuthenticated;
  readonly selectedImage = computed(() => {
    const currentProduct = this.product();
    if (!currentProduct) {
      return '';
    }
    return (
      currentProduct.imageGalleryIds[this.selectedImageIndex()] ??
      currentProduct.imageGalleryIds[0] ??
      currentProduct.imageId
    );
  });
  readonly isWishlisted = computed(() => {
    const currentProduct = this.product();
    if (!currentProduct) {
      return false;
    }
    return this.wishlistFacadeService.isInWishlist(currentProduct.id);
  });

  readonly isLightboxOpen = signal(false);

  constructor() {
    effect(() => {
      this.product();
      this.selectedImageIndex.set(0);
    });

    effect((onCleanup) => {
      const currentProduct = this.product();
      if (!this.isBrowser || !currentProduct || currentProduct.imageGalleryIds.length <= 1 || this.isLightboxOpen()) {
        return;
      }

      const intervalId = window.setInterval(() => {
        this.selectNextImage();
      }, 4500);

      onCleanup(() => window.clearInterval(intervalId));
    });

    effect(() => {
      const currentProduct = this.product();
      const fragment = this.route.snapshot.fragment;

      if (!this.isBrowser || !currentProduct || !fragment) {
        return;
      }

      // Wait for the template to render the anchor before scrolling.
      queueMicrotask(() => {
        requestAnimationFrame(() => {
          const target = this.document.getElementById(fragment);
          if (!target) {
            return;
          }

          target.scrollIntoView({ behavior: 'smooth', block: 'start' });
        });
      });
    });
  }

  selectImage(index: number): void {
    this.selectedImageIndex.set(index);
  }

  selectPreviousImage(): void {
    const imageCount = this.product()?.imageGalleryIds.length ?? 0;
    if (imageCount <= 1) {
      return;
    }

    this.selectedImageIndex.update((currentIndex) => (currentIndex - 1 + imageCount) % imageCount);
  }

  selectNextImage(): void {
    const imageCount = this.product()?.imageGalleryIds.length ?? 0;
    if (imageCount <= 1) {
      return;
    }

    this.selectedImageIndex.update((currentIndex) => (currentIndex + 1) % imageCount);
  }

  goBackToGrid(): void {
    void this.router.navigateByUrl('/products');
  }

  onLightboxStateChanged(isOpen: boolean): void {
    this.isLightboxOpen.set(isOpen);
  }

  addToCart(): void {
    const currentProduct = this.product();
    if (!currentProduct) {
      return;
    }
    this.cartFacadeService.addItem(currentProduct.id);
  }

  addSimilarProductToCart(productId: string): void {
    this.cartFacadeService.addItem(productId);
  }

  saveToWishlist(): void {
    const currentProduct = this.product();
    if (!currentProduct) {
      return;
    }
    this.wishlistFacadeService.toggleItem(currentProduct.id);
  }

  saveSimilarProductToWishlist(productId: string): void {
    this.wishlistFacadeService.toggleItem(productId);
  }

  isProductWishlisted(productId: string): boolean {
    return this.wishlistFacadeService.isInWishlist(productId);
  }
  async submitReview(request: CreateProductReviewRequest): Promise<void> {
    const currentProduct = this.product();
    if (!currentProduct) {
      return;
    }

    this.isSubmittingReview.set(true);
    this.reviewError.set(null);

    try {
      await firstValueFrom(this.productApiService.submitProductReview(currentProduct.id, request));
      this.reviewFormResetToken.update((currentValue) => currentValue + 1);
      this.reloadToken.update((currentValue) => currentValue + 1);
    } catch (error) {
      this.reviewError.set(this.resolveReviewErrorMessage(error));
    } finally {
      this.isSubmittingReview.set(false);
    }
  }

  private resolveReviewErrorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse && error.status === 401) {
      return 'Please log in to leave a review.';
    }

    if (error instanceof HttpErrorResponse && error.status === 400) {
      return 'You can review this product only once and only after purchasing it.';
    }

    return 'We could not submit your review right now. Please try again.';
  }

  openSimilarProduct(productSlug: string): void {
    void this.router.navigate(['/product', productSlug]);
  }

  scrollSimilarItems(direction: 'left' | 'right'): void {
    if (!this.isBrowser) {
      return;
    }
    const container = this.similarItemsTrack?.nativeElement;
    if (!container) {
      return;
    }
    const offset = container.clientWidth ? container.clientWidth * 0.8 : 320;
    container.scrollBy({ left: direction === 'left' ? -offset : offset, behavior: 'smooth' });
  }

  reviewLabel(reviewCount: number): string {
    return reviewCount === 1 ? 'review' : 'reviews';
  }
}
