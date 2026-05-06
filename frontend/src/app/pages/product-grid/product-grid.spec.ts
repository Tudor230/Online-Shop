import { convertToParamMap, ActivatedRoute, Router } from '@angular/router';
import { TestBed } from '@angular/core/testing';
import { BehaviorSubject, Subject } from 'rxjs';
import { CartFacadeService } from '../../core/cart/cart-facade.service';
import { ProductApiService } from '../../core/products/product-api.service';
import { type ProductSearchPage, type ProductSummary } from '../../core/products/product.types';
import { ProductGridComponent } from './product-grid';

const sampleProduct: ProductSummary = {
  id: 'prod-1',
  category: 'Electronics',
  title: 'Gaming Controller',
  rating: 4.8,
  reviewCount: 128,
  price: 79.99,
  imageId: 'image-1'
};

function buildPage(overrides: Partial<ProductSearchPage> = {}): ProductSearchPage {
  return {
    items: [sampleProduct],
    page: 1,
    size: 25,
    totalItems: 100,
    totalPages: 4,
    hasPrevious: false,
    hasNext: true,
    ...overrides
  };
}

describe('ProductGridComponent', () => {
  let queryParams$: BehaviorSubject<ReturnType<typeof convertToParamMap>>;
  let responseSubjects: Subject<ProductSearchPage>[];
  let getProductsCalls: Array<{ query?: string; page?: number; size?: number }>;
  let navigateCalls: Array<unknown[]>;

  const getPageSizeButtons = (fixture: ReturnType<typeof TestBed.createComponent<ProductGridComponent>>) =>
    Array.from(fixture.nativeElement.querySelectorAll('button'))
      .filter((button) => ['25', '50', '100'].includes((button as HTMLButtonElement).textContent?.trim() ?? ''))
      .map((button) => button as HTMLButtonElement);

  beforeEach(async () => {
    queryParams$ = new BehaviorSubject(convertToParamMap({ page: '1', size: '25' }));
    responseSubjects = [];
    getProductsCalls = [];
    navigateCalls = [];

    const productApiService = {
      getProducts: (options: { query?: string; page?: number; size?: number } = {}) => {
        getProductsCalls.push(options);
        const response$ = new Subject<ProductSearchPage>();
        responseSubjects.push(response$);
        return response$.asObservable();
      }
    } as Partial<ProductApiService>;

    const router = {
      navigate: (...args: unknown[]) => {
        navigateCalls.push(args);
        return Promise.resolve(true);
      }
    } as Partial<Router>;

    await TestBed.configureTestingModule({
      imports: [ProductGridComponent],
      providers: [
        { provide: ActivatedRoute, useValue: { queryParamMap: queryParams$.asObservable() } },
        { provide: Router, useValue: router },
        { provide: ProductApiService, useValue: productApiService },
        {
          provide: CartFacadeService,
          useValue: {
            addItem: () => undefined
          }
        }
      ]
    }).compileComponents();
  });

  it('should keep the page size selector in sync with the URL and offer 25-based sizes', async () => {
    const fixture = TestBed.createComponent(ProductGridComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(getProductsCalls).toEqual([{ query: '', page: 1, size: 25 }]);
    expect(responseSubjects).toHaveLength(1);

    responseSubjects[0].next(buildPage({ page: 1, size: 25 }));
    responseSubjects[0].complete();
    fixture.detectChanges();

    const pageSizeButtons = getPageSizeButtons(fixture);

    expect(pageSizeButtons.map((button) => button.textContent?.trim())).toEqual(['25', '50', '100']);
    expect(pageSizeButtons.find((button) => button.getAttribute('aria-pressed') === 'true')?.textContent?.trim()).toBe('25');

    queryParams$.next(convertToParamMap({ page: '1', size: '50' }));
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(getProductsCalls.at(-1)).toEqual({ query: '', page: 1, size: 50 });
    expect(responseSubjects).toHaveLength(2);
    expect(fixture.componentInstance.pageSize()).toBe(50);
    expect(fixture.componentInstance.currentPage()).toBe(1);
  });

  it('should normalize unsupported page sizes to 25', () => {
    const fixture = TestBed.createComponent(ProductGridComponent);
    fixture.detectChanges();

    queryParams$.next(convertToParamMap({ page: '2', size: '37' }));
    fixture.detectChanges();

    expect(getProductsCalls.at(-1)).toEqual({ query: '', page: 2, size: 25 });
    expect(navigateCalls).toEqual([]);
  });
});








