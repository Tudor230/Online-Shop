import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { keycloakConfig } from '../../keycloak.config';
import { ProductDetails, ProductSearchPage } from './product.types';

@Injectable({ providedIn: 'root' })
export class ProductApiService {
  private readonly httpClient = inject(HttpClient);
  private readonly productsBaseUrl = `${keycloakConfig.backendApiUrl}/products`;

  getProducts(options: { query?: string; page?: number; size?: number } = {}): Observable<ProductSearchPage> {
    const { query, page = 1, size = 25 } = options;
    let params = new HttpParams().set('page', String(page)).set('size', String(size));

    const trimmedQuery = query?.trim() ?? '';
    if (trimmedQuery) {
      params = params.set('q', trimmedQuery);
    }

    return this.httpClient.get<ProductSearchPage>(this.productsBaseUrl, { params });
  }

  getProductById(productId: string): Observable<ProductDetails> {
    return this.httpClient.get<ProductDetails>(`${this.productsBaseUrl}/${productId}`);
  }
}
