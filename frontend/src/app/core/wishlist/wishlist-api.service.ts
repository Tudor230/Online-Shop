import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { keycloakConfig } from '../../keycloak.config';
import { WishlistState } from './wishlist.types';

@Injectable({ providedIn: 'root' })
export class WishlistApiService {
  private readonly httpClient = inject(HttpClient);
  private readonly wishlistBaseUrl = `${keycloakConfig.backendApiUrl}/wishlist`;

  getWishlist(): Observable<WishlistState> {
    return this.httpClient.get<WishlistState>(this.wishlistBaseUrl);
  }

  addItem(productId: string): Observable<WishlistState> {
    return this.httpClient.post<WishlistState>(`${this.wishlistBaseUrl}/items/${productId}`, {});
  }

  removeItem(productId: string): Observable<WishlistState> {
    return this.httpClient.delete<WishlistState>(`${this.wishlistBaseUrl}/items/${productId}`);
  }
}
