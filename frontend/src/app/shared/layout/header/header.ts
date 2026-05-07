import { Component, ElementRef, HostListener, ViewChild, effect, inject } from '@angular/core';
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
  imports: [RouterLink, ReactiveFormsModule, CartSidebarComponent],
  templateUrl: './header.html'
})
export class HeaderComponent {
  @ViewChild('profileMenu') private profileMenu?: ElementRef<HTMLDetailsElement>;

  private readonly router = inject(Router);
  private readonly keycloakAuthService = inject(KeycloakAuthService);
  private readonly router = inject(Router);
  readonly authState = inject(AuthStateService);
  readonly cartFacade = inject(CartFacadeService);
  readonly wishlistFacade = inject(WishlistFacadeService);
  readonly searchControl = new FormControl('', { nonNullable: true });

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
      return;
    }

    this.closeProfileMenu();
  }

  closeProfileMenu(): void {
    this.profileMenu?.nativeElement.removeAttribute('open');
  }

  openCartSidebar(): void {
    this.cartFacade.openSidebar();
  }

  closeCartSidebar(): void {
    this.cartFacade.closeSidebar();
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

  openCartProduct(productId: string): void {
    this.closeCartSidebar();
    void this.router.navigate(['/product', productId]);
  }

  openWishlist(): void {
    void this.router.navigate(['/wishlist']);
  }
}
