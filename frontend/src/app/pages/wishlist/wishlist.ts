import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { CartFacadeService } from '../../core/cart/cart-facade.service';
import { CloudinaryTransformOptions } from '../../core/images/cloudinary-url.pipe';
import { WishlistFacadeService } from '../../core/wishlist/wishlist-facade.service';
import { CloudinaryImageFrameComponent } from '../../shared/cloudinary-image-frame/cloudinary-image-frame';
import { PriceDisplayComponent } from '../../shared/price-display/price-display';

@Component({
  selector: 'app-wishlist-page',
  standalone: true,
  imports: [CommonModule, RouterLink, CloudinaryImageFrameComponent, PriceDisplayComponent],
  templateUrl: './wishlist.html'
})
export class WishlistPageComponent {
  private readonly router = inject(Router);
  private readonly cartFacade = inject(CartFacadeService);
  readonly wishlistFacade = inject(WishlistFacadeService);
  readonly wishlistImageOptions: CloudinaryTransformOptions = {
    width: 240,
    height: 180,
    crop: 'pad',
    radius: 10
  };

  openProduct(productSlug: string): void {
    void this.router.navigate(['/product', productSlug]);
  }

  removeFromWishlist(productId: string): void {
    this.wishlistFacade.removeItem(productId);
  }

  addToCart(productId: string): void {
    this.cartFacade.addItem(productId);
  }
}
