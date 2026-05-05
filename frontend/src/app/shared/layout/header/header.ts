import { Component, ElementRef, HostListener, ViewChild, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthStateService } from '../../../core/auth/auth-state.service';
import { CartFacadeService } from '../../../core/cart/cart-facade.service';
import { KeycloakAuthService } from '../../../core/auth/keycloak-auth.service';
import { CartSidebarComponent } from '../cart-sidebar/cart-sidebar';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [RouterLink, CartSidebarComponent],
  templateUrl: './header.html',
})
export class HeaderComponent {
  @ViewChild('profileMenu') private profileMenu?: ElementRef<HTMLDetailsElement>;

  private readonly keycloakAuthService = inject(KeycloakAuthService);
  readonly authState = inject(AuthStateService);
  readonly cartFacade = inject(CartFacadeService);

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
}
