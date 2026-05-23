import { isPlatformBrowser } from '@angular/common';
import { computed, Injectable, effect, inject, PLATFORM_ID, signal } from '@angular/core';
import { firstValueFrom, Observable } from 'rxjs';
import { AuthStateService } from '../auth/auth-state.service';
import { CartApiService } from './cart-api.service';
import { CartItem, CartState } from './cart.types';
import { GuestSessionService } from './guest-session.service';

@Injectable({ providedIn: 'root' })
export class CartFacadeService {
  private readonly emptyCartState: CartState = {
    items: [],
    totalItems: 0,
    subtotal: 0,
    shippingAmount: 0,
    taxAmount: 0,
    totalAmount: 0,
    currencyCode: 'RON'
  };

  private readonly authState = inject(AuthStateService);
  private readonly cartApiService = inject(CartApiService);
  private readonly guestSessionService = inject(GuestSessionService);
  private readonly platformId = inject(PLATFORM_ID);
  private readonly isBrowser = isPlatformBrowser(this.platformId);

  readonly isSidebarOpen = signal(false);
  readonly cartState = signal<CartState>(this.emptyCartState);
  readonly items = computed<CartItem[]>(() => this.cartState().items);
  readonly cartCount = computed(() => this.cartState().totalItems);
  readonly totalPrice = computed(() => this.items().reduce((sum, item) => sum + item.quantity * item.price, 0));
  private wasAuthenticated = false;
  private mutationQueue: Promise<void> = Promise.resolve();

  constructor() {
    effect(() => {
      if (!this.isBrowser) {
        return;
      }
      const isAuthenticated = this.authState.isAuthenticated();
      void this.handleAuthenticationChange(isAuthenticated);
    });
  }

  addItem(productId: string, quantity = 1): void {
    this.applyMutation((sessionId) => this.cartApiService.addItem({ productId, quantity }, sessionId));
  }

  incrementItemQuantity(productId: string): void {
    const item = this.items().find((entry) => entry.productId === productId);
    if (!item) {
      return;
    }
    this.setItemQuantity(productId, item.quantity + 1);
  }

  decrementItemQuantity(productId: string): void {
    const item = this.items().find((entry) => entry.productId === productId);
    if (!item) {
      return;
    }

    if (item.quantity <= 1) {
      this.removeItem(productId);
      return;
    }

    this.setItemQuantity(productId, item.quantity - 1);
  }

  setItemQuantity(productId: string, quantity: number): void {
    if (quantity < 1) {
      throw new Error('Quantity must be at least 1');
    }
    this.applyMutation((sessionId) => this.cartApiService.updateItemQuantity(productId, { quantity }, sessionId));
  }

  removeItem(productId: string): void {
    this.applyMutation((sessionId) => this.cartApiService.removeItem(productId, sessionId));
  }

  openSidebar(): void {
    this.isSidebarOpen.set(true);
    void this.refreshCart();
  }

  closeSidebar(): void {
    this.isSidebarOpen.set(false);
  }

  private async refreshCart(): Promise<void> {
    if (!this.isBrowser) {
      return;
    }

    try {
      const cartState = await firstValueFrom(this.cartApiService.getCart(this.getGuestSessionIfNeeded()));
      this.cartState.set(cartState);
    } catch {
      this.cartState.set(this.emptyCartState);
    }
  }

  private async handleAuthenticationChange(isAuthenticated: boolean): Promise<void> {
    const didJustAuthenticate = isAuthenticated && !this.wasAuthenticated;
    this.wasAuthenticated = isAuthenticated;

    if (didJustAuthenticate) {
      const guestSessionId = this.guestSessionService.getSessionId();
      if (guestSessionId) {
        try {
          const cartState = await firstValueFrom(this.cartApiService.claimGuestCart(guestSessionId));
          this.cartState.set(cartState);
          this.guestSessionService.clearSessionId();
          return;
        } catch {
          // Fall back to normal refresh when claim fails.
        }
      }
    }

    await this.refreshCart();
  }

  private applyMutation(mutation: (sessionId?: string) => Observable<CartState>): void {
    if (!this.isBrowser) {
      return;
    }

    this.mutationQueue = this.mutationQueue.then(async () => {
      try {
        const cartState = await firstValueFrom(mutation(this.getGuestSessionIfNeeded()));
        this.cartState.set(cartState);
      } catch {
        // Keep current cart state if mutation fails.
      }
    });
  }

  private getGuestSessionIfNeeded(): string | undefined {
    if (this.authState.isAuthenticated()) {
      return undefined;
    }
    return this.guestSessionService.getOrCreateSessionId() ?? undefined;
  }
}
