import { HttpErrorResponse } from '@angular/common/http';
import { isPlatformBrowser } from '@angular/common';
import { computed, effect, Injectable, inject, PLATFORM_ID, signal } from '@angular/core';
import { firstValueFrom, Observable } from 'rxjs';
import { AuthStateService } from '../auth/auth-state.service';
import { KeycloakAuthService } from '../auth/keycloak-auth.service';
import { WishlistApiService } from './wishlist-api.service';
import { WishlistItem, WishlistState } from './wishlist.types';

@Injectable({ providedIn: 'root' })
export class WishlistFacadeService {
  private readonly authState = inject(AuthStateService);
  private readonly keycloakAuthService = inject(KeycloakAuthService);
  private readonly wishlistApiService = inject(WishlistApiService);
  private readonly platformId = inject(PLATFORM_ID);
  private readonly isBrowser = isPlatformBrowser(this.platformId);

  readonly wishlistState = signal<WishlistState>({ items: [], totalItems: 0 });
  readonly isLoading = signal(false);
  readonly error = signal<string | null>(null);
  readonly items = computed<WishlistItem[]>(() => this.wishlistState().items);
  readonly count = computed(() => this.wishlistState().totalItems);
  private readonly itemIds = computed(() => new Set(this.items().map((item) => item.productId)));
  private mutationQueue: Promise<void> = Promise.resolve();

  constructor() {
    effect(() => {
      if (!this.isBrowser) {
        return;
      }

      const isAuthenticated = this.authState.isAuthenticated();
      if (isAuthenticated) {
        void this.refreshWishlist();
      } else {
        this.wishlistState.set({ items: [], totalItems: 0 });
        this.error.set(null);
      }
    });
  }

  isInWishlist(productId: string): boolean {
    return this.itemIds().has(productId);
  }

  toggleItem(productId: string): void {
    if (!this.ensureAuthenticatedOrPrompt()) {
      return;
    }

    if (this.isInWishlist(productId)) {
      this.removeItem(productId);
      return;
    }

    this.addItem(productId);
  }

  addItem(productId: string): void {
    if (!this.ensureAuthenticatedOrPrompt()) {
      return;
    }

    this.applyMutation(() => this.wishlistApiService.addItem(productId));
  }

  removeItem(productId: string): void {
    if (!this.ensureAuthenticatedOrPrompt()) {
      return;
    }

    this.applyMutation(() => this.wishlistApiService.removeItem(productId));
  }

  private async refreshWishlist(): Promise<void> {
    this.isLoading.set(true);
    this.error.set(null);

    try {
      const wishlistState = await firstValueFrom(this.wishlistApiService.getWishlist());
      this.wishlistState.set(wishlistState);
    } catch (error) {
      this.wishlistState.set({ items: [], totalItems: 0 });
      this.error.set(this.resolveErrorMessage(error, 'Could not load your wishlist. Please refresh and try again.'));
    } finally {
      this.isLoading.set(false);
    }
  }

  private applyMutation(mutation: () => Observable<WishlistState>): void {
    this.mutationQueue = this.mutationQueue.then(async () => {
      this.error.set(null);
      try {
        const wishlistState = await firstValueFrom(mutation());
        this.wishlistState.set(wishlistState);
      } catch (error) {
        this.error.set(this.resolveErrorMessage(error, 'Could not update your wishlist. Please try again.'));
      }
    });
  }

  private ensureAuthenticatedOrPrompt(): boolean {
    if (this.authState.isAuthenticated()) {
      return true;
    }

    void this.keycloakAuthService.login();
    return false;
  }

  private resolveErrorMessage(error: unknown, fallbackMessage: string): string {
    if (error instanceof HttpErrorResponse) {
      if (error.status === 401) {
        void this.keycloakAuthService.login();
        return 'Your session has expired. Please log in again.';
      }

      if (typeof error.error === 'string' && error.error.trim()) {
        const normalizedText = error.error.replace(/<[^>]*>/g, ' ').replace(/\s+/g, ' ').trim();
        if (normalizedText) {
          return normalizedText.length > 240 ? `${normalizedText.slice(0, 240)}...` : normalizedText;
        }
      }

      const backendMessage = (error.error as { message?: string } | null)?.message;
      if (backendMessage && backendMessage.trim()) {
        return backendMessage;
      }

      if (error.status === 0) {
        return 'Wishlist request failed due to a network/CORS issue. Please verify backend is running and accessible.';
      }

      return `Wishlist request failed (${error.status} ${error.statusText || 'HTTP error'}).`;
    }

    return fallbackMessage;
  }
}
