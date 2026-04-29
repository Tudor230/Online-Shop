import { CommonModule } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { catchError, map, of, startWith } from 'rxjs';
import { AuthStateService } from '../../core/auth/auth-state.service';
import { KeycloakAuthService } from '../../core/auth/keycloak-auth.service';
import { OrderApiService } from '../../core/orders/order-api.service';
import { OrderHistoryEntry, OrderStatus } from '../../core/orders/order.types';

@Component({
  selector: 'app-order-history-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './order-history.html'
})
export class OrderHistoryPageComponent {
  private readonly orderApiService = inject(OrderApiService);
  private readonly authState = inject(AuthStateService);
  private readonly keycloakAuthService = inject(KeycloakAuthService);

  readonly isAuthenticated = this.authState.isAuthenticated;

  private readonly orderHistoryState = toSignal(
    this.orderApiService.getOrderHistory().pipe(
      map((orders) => ({ isLoading: false, orders, error: null as string | null })),
      startWith({ isLoading: true, orders: [] as OrderHistoryEntry[], error: null as string | null }),
      catchError(() =>
        of({
          isLoading: false,
          orders: [] as OrderHistoryEntry[],
          error: 'Could not load your order history. Please refresh and try again.'
        })
      )
    ),
    {
      initialValue: { isLoading: true, orders: [] as OrderHistoryEntry[], error: null as string | null }
    }
  );

  readonly isLoading = computed(() => this.orderHistoryState().isLoading);
  readonly orders = computed(() => this.orderHistoryState().orders);
  readonly error = computed(() => this.orderHistoryState().error);

  async login(): Promise<void> {
    await this.keycloakAuthService.login();
  }

  statusLabel(status: OrderStatus): string {
    return status.charAt(0) + status.slice(1).toLowerCase();
  }

  statusClass(status: OrderStatus): string {
    switch (status) {
      case 'DELIVERED':
        return 'bg-emerald-500/20 text-emerald-300';
      case 'SHIPPED':
        return 'bg-primary/20 text-primary';
      case 'PROCESSING':
      case 'PAID':
        return 'bg-warning/20 text-warning';
      case 'CANCELLED':
      case 'RETURNED':
        return 'bg-red-500/20 text-red-300';
      default:
        return 'bg-surface-elevated text-text-secondary';
    }
  }
}

