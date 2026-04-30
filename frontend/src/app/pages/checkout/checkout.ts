import { CommonModule, CurrencyPipe, isPlatformBrowser } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, PLATFORM_ID, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { AuthStateService } from '../../core/auth/auth-state.service';
import { KeycloakAuthService } from '../../core/auth/keycloak-auth.service';
import { CartApiService } from '../../core/cart/cart-api.service';
import { CartItem } from '../../core/cart/cart.types';
import { CheckoutApiService } from '../../core/checkout/checkout-api.service';
import { OrderApiService } from '../../core/orders/order-api.service';
import { OrderHistoryEntry } from '../../core/orders/order.types';
import { ProfileApiService } from '../../core/profile/profile-api.service';
import { Address, Profile } from '../../core/profile/profile.types';

@Component({
  selector: 'app-checkout-page',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, CurrencyPipe],
  templateUrl: './checkout.html'
})
export class CheckoutPageComponent {
  private readonly authState = inject(AuthStateService);
  private readonly keycloakAuthService = inject(KeycloakAuthService);
  private readonly profileApiService = inject(ProfileApiService);
  private readonly cartApiService = inject(CartApiService);
  private readonly orderApiService = inject(OrderApiService);
  private readonly checkoutApiService = inject(CheckoutApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly platformId = inject(PLATFORM_ID);
  private readonly isBrowser = isPlatformBrowser(this.platformId);

  readonly isAuthenticated = this.authState.isAuthenticated;
  readonly isLoading = signal(true);
  readonly isSubmitting = signal(false);
  readonly error = signal<string | null>(null);
  readonly submitError = signal<string | null>(null);

  readonly mode = signal<'cart' | 'order'>('cart');
  readonly orderId = signal<string | null>(null);
  readonly pendingOrder = signal<OrderHistoryEntry | null>(null);
  readonly cartItems = signal<CartItem[]>([]);
  readonly cartSummary = signal<{ subtotal: number; shippingAmount: number; taxAmount: number; totalAmount: number; currencyCode: string } | null>(null);
  readonly profile = signal<Profile | null>(null);

  readonly shippingAddressId = signal<string | null>(null);
  readonly billingAddressId = signal<string | null>(null);

  readonly addresses = computed(() => this.profile()?.addresses ?? []);
  readonly selectedShippingAddress = computed(() => this.findAddress(this.shippingAddressId()));
  readonly selectedBillingAddress = computed(() => this.findAddress(this.billingAddressId()));
  readonly currencyCode = computed(() => this.pendingOrder()?.currencyCode ?? this.cartSummary()?.currencyCode ?? 'RON');

  readonly displayItems = computed(() => {
    const order = this.pendingOrder();
    if (order) {
      return order.items.map((item) => ({
        key: item.productSlug,
        title: item.title,
        quantity: item.quantity,
        unitPrice: item.unitPrice,
        lineTotal: item.lineTotal
      }));
    }

    return this.cartItems().map((item) => ({
      key: item.productId,
      title: item.title,
      quantity: item.quantity,
      unitPrice: item.price,
      lineTotal: item.price * item.quantity
    }));
  });

  readonly subtotal = computed(() => {
    const order = this.pendingOrder();
    if (order) {
      return order.subtotal;
    }
    return this.displayItems().reduce((sum, item) => sum + item.lineTotal, 0);
  });

  readonly shippingAmount = computed(() => this.pendingOrder()?.shippingAmount ?? this.cartSummary()?.shippingAmount ?? 0);
  readonly taxAmount = computed(() => this.pendingOrder()?.taxAmount ?? this.cartSummary()?.taxAmount ?? 0);
  readonly totalAmount = computed(() => this.pendingOrder()?.totalAmount ?? this.cartSummary()?.totalAmount ?? this.subtotal());

  constructor() {
    void this.loadCheckoutData();
  }

  async login(): Promise<void> {
    await this.keycloakAuthService.login();
  }

  async proceedToStripe(): Promise<void> {
    const shippingAddressId = this.shippingAddressId();
    const billingAddressId = this.billingAddressId();

    if (!shippingAddressId || !billingAddressId) {
      this.submitError.set('Please select both shipping and billing addresses.');
      return;
    }

    this.submitError.set(null);
    this.isSubmitting.set(true);

    try {
      const payload = { shippingAddressId, billingAddressId };
      const orderId = this.orderId();
      const response =
        this.mode() === 'order' && orderId
          ? await firstValueFrom(this.checkoutApiService.createSessionForOrder(orderId, payload))
          : await firstValueFrom(this.checkoutApiService.createSession(payload));

      if (this.isBrowser) {
        window.location.assign(response.checkoutUrl);
      }
    } catch (error: unknown) {
      this.submitError.set(this.extractErrorMessage(error, 'Could not redirect to Stripe. Please try again.'));
    } finally {
      this.isSubmitting.set(false);
    }
  }

  private async loadCheckoutData(): Promise<void> {
    if (!this.isAuthenticated()) {
      this.isLoading.set(false);
      return;
    }

    this.isLoading.set(true);
    this.error.set(null);

    try {
      const orderId = this.route.snapshot.queryParamMap.get('orderId');
      this.orderId.set(orderId);

      const profile = await firstValueFrom(this.profileApiService.getProfile());
      this.profile.set(profile);
      this.shippingAddressId.set(profile.defaultShippingAddressId);
      this.billingAddressId.set(profile.defaultBillingAddressId);

      if (!profile.addresses.length) {
        this.error.set('No saved addresses found. Add at least one address in your profile before checkout.');
        return;
      }

      if (orderId) {
        this.mode.set('order');
        const orders = await firstValueFrom(this.orderApiService.getOrderHistory());
        const order = orders.find((entry) => entry.id === orderId);

        if (!order) {
          this.error.set('Order was not found.');
          return;
        }

        if (order.status !== 'PENDING') {
          this.error.set('Only pending orders can be paid.');
          return;
        }

        this.pendingOrder.set(order);
        return;
      }

      this.mode.set('cart');
      const cart = await firstValueFrom(this.cartApiService.getCart());
      this.cartItems.set(cart.items);
      this.cartSummary.set({
        subtotal: cart.subtotal,
        shippingAmount: cart.shippingAmount,
        taxAmount: cart.taxAmount,
        totalAmount: cart.totalAmount,
        currencyCode: cart.currencyCode
      });

      if (!cart.items.length) {
        this.error.set('Your cart is empty. Add products before checkout.');
      }
    } catch (error: unknown) {
      this.error.set(this.extractErrorMessage(error, 'Could not load checkout details. Please refresh and try again.'));
    } finally {
      this.isLoading.set(false);
    }
  }

  private extractErrorMessage(error: unknown, fallback: string): string {
    if (error instanceof HttpErrorResponse) {
      const apiError = error.error as { message?: string } | null;
      if (apiError?.message && apiError.message.trim().length > 0) {
        return apiError.message;
      }
    }

    return fallback;
  }

  private findAddress(addressId: string | null): Address | null {
    if (!addressId) {
      return null;
    }
    return this.addresses().find((address) => address.id === addressId) ?? null;
  }
}
