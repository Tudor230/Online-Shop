import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { keycloakConfig } from '../../keycloak.config';
import { OrderHistoryEntry } from './order.types';

@Injectable({ providedIn: 'root' })
export class OrderApiService {
  private readonly httpClient = inject(HttpClient);
  private readonly ordersBaseUrl = `${keycloakConfig.backendApiUrl}/orders`;

  getOrderHistory(): Observable<OrderHistoryEntry[]> {
    return this.httpClient.get<OrderHistoryEntry[]>(`${this.ordersBaseUrl}/history`);
  }
}

