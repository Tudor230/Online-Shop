import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { keycloakConfig } from '../../keycloak.config';
import { CartState } from './cart.types';

interface AddCartItemRequest {
  productId: string;
  quantity: number;
}

interface UpdateCartItemQuantityRequest {
  quantity: number;
}

@Injectable({ providedIn: 'root' })
export class CartApiService {
  private readonly httpClient = inject(HttpClient);
  private readonly cartBaseUrl = `${keycloakConfig.backendApiUrl}/cart`;

  getCart(sessionId?: string): Observable<CartState> {
    return this.httpClient.get<CartState>(this.cartBaseUrl, this.buildOptions(sessionId));
  }

  addItem(request: AddCartItemRequest, sessionId?: string): Observable<CartState> {
    return this.httpClient.post<CartState>(`${this.cartBaseUrl}/items`, request, this.buildOptions(sessionId));
  }

  updateItemQuantity(productId: string, request: UpdateCartItemQuantityRequest, sessionId?: string): Observable<CartState> {
    return this.httpClient.patch<CartState>(`${this.cartBaseUrl}/items/${productId}`, request, this.buildOptions(sessionId));
  }

  removeItem(productId: string, sessionId?: string): Observable<CartState> {
    return this.httpClient.delete<CartState>(`${this.cartBaseUrl}/items/${productId}`, this.buildOptions(sessionId));
  }

  claimGuestCart(sessionId: string): Observable<CartState> {
    return this.httpClient.post<CartState>(`${this.cartBaseUrl}/claim`, {}, this.buildOptions(sessionId));
  }

  private buildOptions(sessionId?: string): { headers?: HttpHeaders } {
    if (!sessionId) {
      return {};
    }
    return {
      headers: new HttpHeaders({
        'X-Session-Id': sessionId
      })
    };
  }
}
