import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { keycloakConfig } from '../../keycloak.config';
import {
  CheckoutStatusResponse,
  CreateCheckoutSessionRequest,
  CreateCheckoutSessionResponse
} from './checkout.types';

@Injectable({ providedIn: 'root' })
export class CheckoutApiService {
  private readonly httpClient = inject(HttpClient);
  private readonly checkoutBaseUrl = `${keycloakConfig.backendApiUrl}/checkout`;

  createSession(payload: CreateCheckoutSessionRequest): Observable<CreateCheckoutSessionResponse> {
    return this.httpClient.post<CreateCheckoutSessionResponse>(`${this.checkoutBaseUrl}/session`, payload);
  }

  createSessionForOrder(orderId: string, payload: CreateCheckoutSessionRequest): Observable<CreateCheckoutSessionResponse> {
    return this.httpClient.post<CreateCheckoutSessionResponse>(`${this.checkoutBaseUrl}/session/order/${orderId}`, payload);
  }

  getCheckoutStatus(orderId: string): Observable<CheckoutStatusResponse> {
    return this.httpClient.get<CheckoutStatusResponse>(`${this.checkoutBaseUrl}/status/${orderId}`);
  }
}
