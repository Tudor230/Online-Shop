import { CurrencyPipe, isPlatformBrowser } from '@angular/common';
import { Component, ElementRef, HostListener, PLATFORM_ID, ViewChild, effect, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { NavigationEnd, Router, RouterLink } from '@angular/router';
import { filter, map, startWith } from 'rxjs';
import { AuthStateService } from '../../../core/auth/auth-state.service';
import { CartFacadeService } from '../../../core/cart/cart-facade.service';
import { KeycloakAuthService } from '../../../core/auth/keycloak-auth.service';
import { WishlistFacadeService } from '../../../core/wishlist/wishlist-facade.service';
import { CartSidebarComponent } from '../cart-sidebar/cart-sidebar';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [RouterLink, ReactiveFormsModule, CartSidebarComponent, CurrencyPipe],
  templateUrl: './header.html'
})
export class HeaderComponent {
  @ViewChild('profileMenu') private profileMenu?: ElementRef<HTMLDetailsElement>;
  @ViewChild('cartPreview') private cartPreview?: ElementRef<HTMLElement>;
  @ViewChild('cartButton') private cartButton?: ElementRef<HTMLButtonElement>;
  @ViewChild('cartPreviewContinue') private cartPreviewContinue?: ElementRef<HTMLButtonElement>;

  private readonly keycloakAuthService = inject(KeycloakAuthService);
  private readonly router = inject(Router);
  private readonly platformId = inject(PLATFORM_ID);
  private readonly isBrowser = isPlatformBrowser(this.platformId);
  readonly authState = inject(AuthStateService);
  readonly cartFacade = inject(CartFacadeService);
  readonly wishlistFacade = inject(WishlistFacadeService);
  readonly searchControl = new FormControl('', { nonNullable: true });
  isCheckoutInProgress = false;
  readonly isCartPreviewOpen = signal(false);

  private readonly currentUrlFromRoute = toSignal(
    this.router.events.pipe(
      filter((event) => event instanceof NavigationEnd),
      startWith(null),
      map(() => this.router.url)
    ),
    { initialValue: this.router.url }
  );

  private readonly searchTermFromRoute = toSignal(
    this.router.events.pipe(
      filter((event) => event instanceof NavigationEnd),
      startWith(null),
      map(() => (this.router.parseUrl(this.router.url).queryParams['q'] ?? '').trim())
    ),
    { initialValue: '' }
  );

  constructor() {
    effect(() => {
      this.searchControl.setValue(this.searchTermFromRoute(), { emitEvent: false });
    });

    effect(() => {
      this.currentUrlFromRoute();
      if (this.isCheckoutInProgress) {
        this.isCheckoutInProgress = false;
      }
    });

    effect(() => {
      const token = this.cartFacade.lastAddedToken();
      if (token > 0) {
        this.isCartPreviewOpen.set(true);
      }
    });

    effect(() => {
      if (!this.isCartPreviewOpen()) {
        return;
      }
      this.focusCartPreview();
    });
  }

  submitSearch(event: Event): void {
    event.preventDefault();
    const query = this.searchControl.value.trim();
    void this.router.navigate(['/products'], {
      queryParams: query ? { q: query } : {}
    });
  }

  async login(): Promise<void> {
    await this.keycloakAuthService.login();
  }

  async logout(): Promise<void> {
    this.closeProfileMenu();
    await this.keycloakAuthService.logout();
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    const menu = this.profileMenu?.nativeElement;
    const target = event.target as Node | null;

    if (!menu?.open || !target || menu.contains(target)) {
      this.closeCartPreviewIfNeeded(target);
      return;
    }

    this.closeProfileMenu();
    this.closeCartPreviewIfNeeded(target);
  }

  @HostListener('document:keydown', ['$event'])
  onDocumentKeydown(event: KeyboardEvent): void {
    if (!this.isCartPreviewOpen() || event.key !== 'Escape') {
      return;
    }
    event.preventDefault();
    this.closeCartPreview();
    this.focusCartButton();
  }

  closeProfileMenu(): void {
    this.profileMenu?.nativeElement.removeAttribute('open');
  }

  openCartSidebar(): void {
    this.closeCartPreview();
    this.cartFacade.openSidebar();
  }

  closeCartSidebar(): void {
    this.cartFacade.closeSidebar();
  }

  closeCartPreview(): void {
    this.isCartPreviewOpen.set(false);
  }

  incrementCartItem(productId: string): void {
    this.cartFacade.incrementItemQuantity(productId);
  }

  decrementCartItem(productId: string): void {
    this.cartFacade.decrementItemQuantity(productId);
  }

  removeCartItem(productId: string): void {
    this.cartFacade.removeItem(productId);
  }

  async startCheckout(): Promise<void> {
    if (this.isCheckoutInProgress) {
      return;
    }

    if (!this.authState.isAuthenticated()) {
      await this.login();
      return;
    }

    this.isCheckoutInProgress = true;
    this.closeCartSidebar();
    try {
      const didNavigate = await this.router.navigate(['/checkout']);
      if (!didNavigate) {
        this.isCheckoutInProgress = false;
      }
    } catch {
      this.isCheckoutInProgress = false;
    }
  }

  openCartProduct(productSlug: string): void {
    this.closeCartSidebar();
    this.closeCartPreview();
    void this.router.navigate(['/product', productSlug]);
  }

  openWishlist(): void {
    void this.router.navigate(['/wishlist']);
  }

  private closeCartPreviewIfNeeded(target: Node | null): void {
    if (!this.isCartPreviewOpen() || !target) {
      return;
    }
    const preview = this.cartPreview?.nativeElement;
    const button = this.cartButton?.nativeElement;
    if ((preview && preview.contains(target)) || (button && button.contains(target))) {
      return;
    }
    this.closeCartPreview();
  }

  private focusCartPreview(): void {
    if (!this.isBrowser) {
      return;
    }
    queueMicrotask(() => {
      requestAnimationFrame(() => {
        const focusTarget = this.cartPreviewContinue?.nativeElement ?? this.cartPreview?.nativeElement;
        focusTarget?.focus({ preventScroll: true });
      });
    });
  }

  private focusCartButton(): void {
    if (!this.isBrowser) {
      return;
    }
    this.cartButton?.nativeElement.focus({ preventScroll: true });
  }
}
