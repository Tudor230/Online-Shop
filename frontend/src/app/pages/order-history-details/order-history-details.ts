import { CommonModule, isPlatformBrowser } from '@angular/common';
import { Component, PLATFORM_ID, computed, inject } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { combineLatest, catchError, map, of, startWith, switchMap } from 'rxjs';
import { AuthStateService } from '../../core/auth/auth-state.service';
import { KeycloakAuthService } from '../../core/auth/keycloak-auth.service';
import { OrderApiService } from '../../core/orders/order-api.service';
import { OrderDetailsEntry, OrderStatus } from '../../core/orders/order.types';
import { CloudinaryImageFrameComponent } from '../../shared/cloudinary-image-frame/cloudinary-image-frame';

@Component({
  selector: 'app-order-history-details-page',
  standalone: true,
  imports: [CommonModule, RouterLink, CloudinaryImageFrameComponent],
  templateUrl: './order-history-details.html'
})
export class OrderHistoryDetailsPageComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly orderApiService = inject(OrderApiService);
  private readonly authState = inject(AuthStateService);
  private readonly keycloakAuthService = inject(KeycloakAuthService);
  private readonly platformId = inject(PLATFORM_ID);
  private readonly isBrowser = isPlatformBrowser(this.platformId);

  readonly isAuthenticated = this.authState.isAuthenticated;

  private readonly detailsState = toSignal(
    combineLatest([toObservable(this.isAuthenticated), this.route.paramMap.pipe(map((params) => params.get('orderSlug')))]).pipe(
      switchMap(([isAuthenticated, orderSlug]) => {
        if (!this.isBrowser || !isAuthenticated || !orderSlug) {
          return of({ isLoading: false, order: null as OrderDetailsEntry | null, error: null as string | null });
        }

        return this.orderApiService.getOrderBySlug(orderSlug).pipe(
          map((order) => ({ isLoading: false, order, error: null as string | null })),
          startWith({ isLoading: true, order: null as OrderDetailsEntry | null, error: null as string | null }),
          catchError(() =>
            of({
              isLoading: false,
              order: null as OrderDetailsEntry | null,
              error: 'Could not load this order right now. Please go back and try again.'
            })
          )
        );
      })
    ),
    {
      initialValue: { isLoading: false, order: null as OrderDetailsEntry | null, error: null as string | null }
    }
  );

  readonly isLoading = computed(() => this.detailsState().isLoading);
  readonly order = computed(() => this.detailsState().order);
  readonly error = computed(() => this.detailsState().error);

  async login(): Promise<void> {
    await this.keycloakAuthService.login();
  }

  statusLabel(status: OrderStatus): string {
    switch (status) {
      case 'PENDING': return 'Pending Payment';
      case 'PAID': return 'Paid';
      case 'PROCESSING': return 'Processing';
      case 'SHIPPED': return 'Shipped';
      case 'DELIVERED': return 'Delivered';
      case 'CANCELLED': return 'Cancelled';
      case 'RETURNED': return 'Returned';
      default: return status;
    }
  }

  statusClass(status: OrderStatus): string {
    switch (status) {
      case 'DELIVERED':
        return 'bg-[#15be53]/20 text-[#15be53]';
      case 'SHIPPED':
        return 'bg-[#2b91df]/20 text-[#2b91df]';
      case 'PROCESSING':
      case 'PAID':
        return 'bg-[#9b6829]/20 text-[#9b6829]';
      case 'PENDING':
        return 'bg-[#533afd]/15 text-[#533afd]';
      case 'CANCELLED':
      case 'RETURNED':
        return 'bg-[#ea2261]/20 text-[#ea2261]';
      default:
        return 'bg-[#f6f9fc] text-[#64748d]';
    }
  }
}

