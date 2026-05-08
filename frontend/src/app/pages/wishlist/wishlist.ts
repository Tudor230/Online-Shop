import { CommonModule, CurrencyPipe } from '@angular/common';
import { Component, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { CartFacadeService } from '../../core/cart/cart-facade.service';
import { WishlistFacadeService } from '../../core/wishlist/wishlist-facade.service';

@Component({
  selector: 'app-wishlist-page',
  standalone: true,
  imports: [CommonModule, CurrencyPipe, RouterLink],
  templateUrl: './wishlist.html'
})
export class WishlistPageComponent {
  private readonly router = inject(Router);
  private readonly cartFacade = inject(CartFacadeService);
  readonly wishlistFacade = inject(WishlistFacadeService);

  openProduct(productSlug: string): void {
    void this.router.navigate(['/product', productSlug]);
  }

  removeFromWishlist(productSlug: string): void {
    this.wishlistFacade.removeItem(productSlug);
  }

  addToCart(productSlug: string): void {
    this.cartFacade.addItem(productSlug);
  }
}
