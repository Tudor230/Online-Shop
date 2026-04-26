import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { keycloakConfig } from '../../keycloak.config';
import { ProductDetails, ProductSummary } from './product.types';

@Injectable({ providedIn: 'root' })
export class ProductApiService {
  private readonly httpClient = inject(HttpClient);
  private readonly productsBaseUrl = `${keycloakConfig.backendApiUrl}/products`;

  getProducts(): Observable<ProductSummary[]> {
    return this.httpClient.get<ProductSummary[]>(this.productsBaseUrl);
  }

  getProductById(productId: string): Observable<ProductDetails> {
    return this.httpClient.get<ProductDetails>(`${this.productsBaseUrl}/${productId}`);
  }
}

