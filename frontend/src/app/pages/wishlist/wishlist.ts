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

  openProduct(productId: string): void {
    void this.router.navigate(['/product', productId]);
  }

  removeFromWishlist(productId: string): void {
    this.wishlistFacade.removeItem(productId);
  }

  addToCart(productId: string): void {
    this.cartFacade.addItem(productId);
  }
}
