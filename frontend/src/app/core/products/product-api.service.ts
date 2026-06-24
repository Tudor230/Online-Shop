import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { keycloakConfig } from '../../keycloak.config';
import { CategoryTreeNode, CreateProductReviewRequest, ProductDetails, ProductSearchPage, ProductSummary } from './product.types';

@Injectable({ providedIn: 'root' })
export class ProductApiService {
  private readonly httpClient = inject(HttpClient);
  private readonly productsBaseUrl = `${keycloakConfig.backendApiUrl}/products`;
  private readonly categoriesBaseUrl = `${keycloakConfig.backendApiUrl}/categories`;

  getProducts(options: { query?: string; category?: string; page?: number; size?: number } = {}): Observable<ProductSearchPage> {
    const { query, category, page = 1, size = 25 } = options;
    let params = new HttpParams().set('page', String(page)).set('size', String(size));

    const trimmedQuery = query?.trim() ?? '';
    if (trimmedQuery) {
      params = params.set('q', trimmedQuery);
    }

    const trimmedCategory = category?.trim() ?? '';
    if (trimmedCategory) {
      params = params.set('category', trimmedCategory);
    }

    return this.httpClient.get<ProductSearchPage>(this.productsBaseUrl, { params });
  }

  getCategories(): Observable<CategoryTreeNode[]> {
    return this.httpClient.get<CategoryTreeNode[]>(this.categoriesBaseUrl);
  }

  getProductBySlug(slug: string): Observable<ProductDetails> {
    return this.httpClient.get<ProductDetails>(`${this.productsBaseUrl}/${slug}`);
  }

  getSimilarProducts(slug: string, size = 8): Observable<ProductSummary[]> {
    const params = new HttpParams().set('size', String(size));
    return this.httpClient.get<ProductSummary[]>(`${this.productsBaseUrl}/${slug}/similar`, { params });
  }

  submitProductReview(productId: string, request: CreateProductReviewRequest): Observable<ProductDetails> {
    return this.httpClient.post<ProductDetails>(`${this.productsBaseUrl}/${productId}/reviews`, request);
  }
}
