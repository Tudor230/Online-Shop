import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { keycloakConfig } from '../../keycloak.config';
import { OrderDetailsEntry, OrderHistoryEntry } from './order.types';

@Injectable({ providedIn: 'root' })
export class OrderApiService {
  private readonly httpClient = inject(HttpClient);
  private readonly ordersBaseUrl = `${keycloakConfig.backendApiUrl}/orders`;

  getOrderHistory(): Observable<OrderHistoryEntry[]> {
    return this.httpClient.get<OrderHistoryEntry[]>(`${this.ordersBaseUrl}/history`);
  }

  getOrderBySlug(orderSlug: string): Observable<OrderDetailsEntry> {
    return this.httpClient.get<OrderDetailsEntry>(`${this.ordersBaseUrl}/${orderSlug}`);
  }

  cancelOrder(orderId: string): Observable<OrderHistoryEntry> {
    return this.httpClient.patch<OrderHistoryEntry>(`${this.ordersBaseUrl}/${orderId}/cancel`, {});
  }
}

