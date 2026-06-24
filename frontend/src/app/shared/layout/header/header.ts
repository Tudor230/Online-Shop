import { isPlatformBrowser } from '@angular/common';
import { Component, ElementRef, HostListener, PLATFORM_ID, ViewChild, computed, effect, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { NavigationEnd, Router, RouterLink } from '@angular/router';
import { filter, map, startWith, debounceTime, distinctUntilChanged, skip } from 'rxjs';
import { AuthStateService } from '../../../core/auth/auth-state.service';
import { CartFacadeService } from '../../../core/cart/cart-facade.service';
import { KeycloakAuthService } from '../../../core/auth/keycloak-auth.service';
import { WishlistFacadeService } from '../../../core/wishlist/wishlist-facade.service';
import { CartSidebarComponent } from '../cart-sidebar/cart-sidebar';
import { PriceDisplayComponent } from '../../price-display/price-display';

const SEARCH_HISTORY_KEY = 'os_search_history';
const MAX_HISTORY_ITEMS = 8;

function loadSearchHistory(): string[] {
  try {
    const raw = localStorage.getItem(SEARCH_HISTORY_KEY);
    if (!raw) return [];
    const parsed = JSON.parse(raw);
    return Array.isArray(parsed) ? parsed.filter((s: unknown): s is string => typeof s === 'string' && s.trim().length > 0) : [];
  } catch {
    return [];
  }
}

function saveSearchHistory(history: string[]): void {
  try {
    localStorage.setItem(SEARCH_HISTORY_KEY, JSON.stringify(history));
  } catch { /* quota exceeded, ignore */ }
}

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [RouterLink, ReactiveFormsModule, CartSidebarComponent, PriceDisplayComponent],
  templateUrl: './header.html'
})
export class HeaderComponent {
  @ViewChild('profileMenu') private profileMenu?: ElementRef<HTMLDetailsElement>;
  @ViewChild('cartPreview') private cartPreview?: ElementRef<HTMLElement>;
  @ViewChild('cartButton') private cartButton?: ElementRef<HTMLButtonElement>;
  @ViewChild('cartPreviewContinue') private cartPreviewContinue?: ElementRef<HTMLButtonElement>;
  @ViewChild('searchForm') private searchForm?: ElementRef<HTMLElement>;

  private readonly keycloakAuthService = inject(KeycloakAuthService);
  private readonly router = inject(Router);
  private readonly platformId = inject(PLATFORM_ID);
  private readonly isBrowser = isPlatformBrowser(this.platformId);
  readonly authState = inject(AuthStateService);
  readonly cartFacade = inject(CartFacadeService);
  readonly wishlistFacade = inject(WishlistFacadeService);
  readonly searchControl = new FormControl('', { nonNullable: true });
  readonly searchHistory = signal<string[]>(this.isBrowser ? loadSearchHistory() : []);
  private readonly searchValue = toSignal(
    this.searchControl.valueChanges.pipe(startWith(this.searchControl.value)),
    { initialValue: this.searchControl.value }
  );
  readonly filteredHistory = computed(() => {
    const query = (this.searchValue() ?? '').trim().toLowerCase();
    if (!query) return this.searchHistory();
    return this.searchHistory().filter((term) => term.toLowerCase().includes(query));
  });
  readonly showSearchHistory = signal(false);
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

    this.searchControl.valueChanges.pipe(
      skip(1),
      debounceTime(350),
      distinctUntilChanged(),
      filter(() => this.isBrowser)
    ).subscribe((query) => {
      const trimmed = (query ?? '').trim();
      void this.router.navigate(['/products'], {
        queryParams: { q: trimmed || null },
        queryParamsHandling: 'merge',
        replaceUrl: true
      });
    });
  }

  submitSearch(event: Event): void {
    event.preventDefault();
    const query = this.searchControl.value.trim();
    if (query) {
      const history = this.searchHistory().filter((term) => term !== query);
      history.unshift(query);
      const trimmed = history.slice(0, MAX_HISTORY_ITEMS);
      this.searchHistory.set(trimmed);
      if (this.isBrowser) saveSearchHistory(trimmed);
    }
    this.showSearchHistory.set(false);
    void this.router.navigate(['/products'], {
      queryParams: { q: query || null },
      queryParamsHandling: 'merge'
    });
  }

  onSearchFocus(): void {
    this.showSearchHistory.set(true);
  }

  onHistoryItemClick(query: string): void {
    this.searchControl.setValue(query);
    this.showSearchHistory.set(false);
    const history = this.searchHistory().filter((term) => term !== query);
    history.unshift(query);
    const trimmed = history.slice(0, MAX_HISTORY_ITEMS);
    this.searchHistory.set(trimmed);
    if (this.isBrowser) saveSearchHistory(trimmed);
    void this.router.navigate(['/products'], { queryParams: { q: query }, queryParamsHandling: 'merge' });
  }

  removeHistoryItem(event: Event, query: string): void {
    event.stopPropagation();
    const history = this.searchHistory().filter((term) => term !== query);
    this.searchHistory.set(history);
    if (this.isBrowser) saveSearchHistory(history);
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

    this.closeSearchHistoryIfNeeded(target);

    if (!menu?.open || !target || menu.contains(target)) {
      this.closeCartPreviewIfNeeded(target);
      return;
    }

    this.closeProfileMenu();
    this.closeCartPreviewIfNeeded(target);
  }

  @HostListener('document:keydown', ['$event'])
  onDocumentKeydown(event: KeyboardEvent): void {
    if (this.showSearchHistory() && event.key === 'Escape') {
      event.preventDefault();
      this.showSearchHistory.set(false);
      return;
    }
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

  private closeSearchHistoryIfNeeded(target: Node | null): void {
    if (!this.showSearchHistory() || !target) return;
    const form = this.searchForm?.nativeElement;
    if (form && form.contains(target)) return;
    this.showSearchHistory.set(false);
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
