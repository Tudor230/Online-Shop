import { CommonModule, isPlatformBrowser } from '@angular/common';
import { Component, PLATFORM_ID, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { catchError, map, of, startWith, switchMap, takeWhile, timer } from 'rxjs';
import { CheckoutApiService } from '../../core/checkout/checkout-api.service';
import { CheckoutStatusResponse } from '../../core/checkout/checkout.types';

@Component({
  selector: 'app-checkout-success-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './checkout-success.html'
})
export class CheckoutSuccessPageComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly checkoutApiService = inject(CheckoutApiService);
  private readonly platformId = inject(PLATFORM_ID);
  private readonly isBrowser = isPlatformBrowser(this.platformId);

  private readonly orderId = toSignal(this.route.queryParamMap.pipe(map((params) => params.get('orderId'))), {
    initialValue: null
  });

  private readonly checkoutState = toSignal(
    this.route.queryParamMap.pipe(
      map((params) => params.get('orderId')),
      switchMap((orderId) => {
        if (!this.isBrowser || !orderId) {
          return of({
            isLoading: false,
            orderId,
            status: null as CheckoutStatusResponse['status'] | null,
            error: orderId ? null : 'Missing order id in return URL.'
          });
        }

        return timer(0, 2000).pipe(
          switchMap(() => this.checkoutApiService.getCheckoutStatus(orderId)),
          takeWhile((response, index) => response.status === 'PENDING' && index < 60, true),
          map((response) => ({
            isLoading: false,
            orderId,
            status: response.status,
            error: null as string | null
          })),
          startWith({
            isLoading: true,
            orderId,
            status: null as CheckoutStatusResponse['status'] | null,
            error: null as string | null
          }),
          catchError(() =>
            of({
              isLoading: false,
              orderId,
              status: null as CheckoutStatusResponse['status'] | null,
              error: 'Could not verify payment status. Please check your order history in a moment.'
            })
          )
        );
      })
    ),
    {
      initialValue: {
        isLoading: false,
        orderId: null,
        status: null as CheckoutStatusResponse['status'] | null,
        error: null as string | null
      }
    }
  );

  readonly isLoading = computed(() => this.checkoutState().isLoading);
  readonly status = computed(() => this.checkoutState().status);
  readonly error = computed(() => this.checkoutState().error);
  readonly currentOrderId = computed(() => this.checkoutState().orderId ?? this.orderId());
  readonly isPaid = computed(() => this.status() === 'PAID');
  readonly isPending = computed(() => this.status() === 'PENDING' || (this.isLoading() && !this.error()));
}
