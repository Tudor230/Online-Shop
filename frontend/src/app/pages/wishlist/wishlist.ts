import { CommonModule, CurrencyPipe } from '@angular/common';
import { Component, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { CartFacadeService } from '../../core/cart/cart-facade.service';
import { CloudinaryTransformOptions } from '../../core/images/cloudinary-url.pipe';
import { WishlistFacadeService } from '../../core/wishlist/wishlist-facade.service';
import { CloudinaryImageFrameComponent } from '../../shared/cloudinary-image-frame/cloudinary-image-frame';

@Component({
  selector: 'app-wishlist-page',
  standalone: true,
  imports: [CommonModule, CurrencyPipe, RouterLink, CloudinaryImageFrameComponent],
  templateUrl: './wishlist.html'
})
export class WishlistPageComponent {
  private readonly router = inject(Router);
  private readonly cartFacade = inject(CartFacadeService);
  readonly wishlistFacade = inject(WishlistFacadeService);
  readonly wishlistImageOptions: CloudinaryTransformOptions = {
    width: 160,
    height: 160,
    crop: 'limit',
    radius: 10
  };

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
