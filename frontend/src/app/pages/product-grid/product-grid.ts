import { CommonModule } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { catchError, map, of, startWith, switchMap } from 'rxjs';
import { CartFacadeService } from '../../core/cart/cart-facade.service';
import { ProductApiService } from '../../core/products/product-api.service';
import { type ProductSearchPage, type ProductSummary } from '../../core/products/product.types';
import { ProductCardComponent } from '../../shared/product-card/product-card';

interface ProductListState {
  isLoading: boolean;
  hasError: boolean;
  result: ProductSearchPage;
}

interface SearchParams {
  query: string;
  page: number;
  size: number;
}

@Component({
  selector: 'app-product-grid',
  standalone: true,
  imports: [CommonModule, ProductCardComponent],
  templateUrl: './product-grid.html'
})
export class ProductGridComponent {
  private static readonly DEFAULT_PAGE_SIZE = 24;
  private static readonly PAGE_SIZE_OPTIONS = [24, 48, 96] as const;
  private static readonly PAGE_LINK_WINDOW = 1;

  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly productApiService = inject(ProductApiService);
  private readonly cartFacadeService = inject(CartFacadeService);

  private readonly searchParams = toSignal(
    this.route.queryParamMap.pipe(
      map((queryParams) => {
        const rawPage = Number(queryParams.get('page') ?? '1');
        const rawSize = Number(queryParams.get('size') ?? String(ProductGridComponent.DEFAULT_PAGE_SIZE));
        return {
          query: (queryParams.get('q') ?? '').trim(),
          page: Number.isInteger(rawPage) && rawPage > 0 ? rawPage : 1,
          size: this.normalizePageSize(rawSize)
        } as SearchParams;
      })
    ),
    { initialValue: { query: '', page: 1, size: ProductGridComponent.DEFAULT_PAGE_SIZE } as SearchParams }
  );

  private readonly productListState = toSignal(
    toObservable(this.searchParams).pipe(
      switchMap(({ query, page, size }) =>
        this.productApiService.getProducts({ query, page, size }).pipe(
          map((result) => ({ isLoading: false, hasError: false, result })),
          startWith({ isLoading: true, hasError: false, result: this.emptyResult(page, size) }),
          catchError(() => of({ isLoading: false, hasError: true, result: this.emptyResult(page, size) }))
        )
      )
    ),
    {
      initialValue: {
        isLoading: true,
        hasError: false,
        result: this.emptyResult(1, ProductGridComponent.DEFAULT_PAGE_SIZE)
      }
    }
  );

  readonly isLoading = computed(() => this.productListState().isLoading);
  readonly hasError = computed(() => this.productListState().hasError);
  readonly products = computed(() => this.productListState().result.items);
  readonly isSearchActive = computed(() => this.searchParams().query.length > 0);
  readonly currentPage = computed(() => this.productListState().result.page || 1);
  readonly totalPages = computed(() => this.productListState().result.totalPages);
  readonly hasPreviousPage = computed(() => this.productListState().result.hasPrevious);
  readonly hasNextPage = computed(() => this.productListState().result.hasNext);
  readonly totalItems = computed(() => this.productListState().result.totalItems);
  readonly pageSize = computed(() => this.productListState().result.size || this.searchParams().size);
  readonly pageSizeOptions = ProductGridComponent.PAGE_SIZE_OPTIONS;
  readonly shouldShowPagination = computed(() => this.totalPages() > 1);
  readonly resultRangeLabel = computed(() => {
    const totalItems = this.totalItems();
    if (totalItems === 0) {
      return 'Showing 0 results';
    }

    const start = (this.currentPage() - 1) * this.pageSize() + 1;
    const end = Math.min(start + this.products().length - 1, totalItems);
    return `Showing ${start}-${end} of ${totalItems} results`;
  });
  readonly pageLinks = computed(() => this.buildPageLinks(this.currentPage(), this.totalPages()));
  readonly skeletonItems = Array.from({ length: 8 }, (_, index) => index);

  openProductDetails(productId: string): void {
    void this.router.navigate(['/product', productId]);
  }

  addProductToCart(productId: string): void {
    this.cartFacadeService.addItem(productId);
  }

  goToPreviousPage(): void {
    if (!this.hasPreviousPage()) {
      return;
    }

    this.goToPage(this.currentPage() - 1);
  }

  goToNextPage(): void {
    if (!this.hasNextPage()) {
      return;
    }

    this.goToPage(this.currentPage() + 1);
  }

  goToPageNumber(page: number): void {
    if (page < 1 || page > this.totalPages() || page === this.currentPage()) {
      return;
    }

    this.goToPage(page);
  }

  goToPageLink(pageLink: number | string): void {
    if (!this.isPageLink(pageLink)) {
      return;
    }

    this.goToPageNumber(pageLink);
  }

  changePageSize(rawSize: string): void {
    const size = this.normalizePageSize(Number(rawSize));
    this.goToPage(1, size);
  }

  isPageLink(pageLink: number | string): pageLink is number {
    return typeof pageLink === 'number';
  }

  private goToPage(page: number, sizeOverride?: number): void {
    const { query, size } = this.searchParams();
    const nextSize = sizeOverride ?? size;
    void this.router.navigate(['/products'], {
      queryParams: {
        q: query || null,
        page: page > 1 ? page : null,
        size: nextSize === ProductGridComponent.DEFAULT_PAGE_SIZE ? null : nextSize
      }
    });
  }

  private normalizePageSize(rawSize: number): number {
    if (!Number.isInteger(rawSize)) {
      return ProductGridComponent.DEFAULT_PAGE_SIZE;
    }

    return ProductGridComponent.PAGE_SIZE_OPTIONS.includes(rawSize as (typeof ProductGridComponent.PAGE_SIZE_OPTIONS)[number])
      ? rawSize
      : ProductGridComponent.DEFAULT_PAGE_SIZE;
  }

  private buildPageLinks(currentPage: number, totalPages: number): Array<number | string> {
    if (totalPages <= 0) {
      return [];
    }
    if (totalPages <= 7) {
      return Array.from({ length: totalPages }, (_, index) => index + 1);
    }

    const links: Array<number | string> = [1];
    const windowStart = Math.max(2, currentPage - ProductGridComponent.PAGE_LINK_WINDOW);
    const windowEnd = Math.min(totalPages - 1, currentPage + ProductGridComponent.PAGE_LINK_WINDOW);

    if (windowStart > 2) {
      links.push('...');
    }

    for (let page = windowStart; page <= windowEnd; page++) {
      links.push(page);
    }

    if (windowEnd < totalPages - 1) {
      links.push('...');
    }

    links.push(totalPages);
    return links;
  }

  private emptyResult(page: number, size: number): ProductSearchPage {
    return {
      items: [] as ProductSummary[],
      page,
      size,
      totalItems: 0,
      totalPages: 0,
      hasPrevious: page > 1,
      hasNext: false
    };
  }

  saveProductToWishlist(_productId: string): void {}
}
