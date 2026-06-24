import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { ProductSummary } from '../../core/products/product.types';
import { CloudinaryTransformOptions } from '../../core/images/cloudinary-url.pipe';
import { CloudinaryImageFrameComponent } from '../cloudinary-image-frame/cloudinary-image-frame';
import { PriceDisplayComponent } from '../price-display/price-display';
import { StarRatingComponent } from '../star-rating/star-rating';

@Component({
  selector: 'app-product-card',
  standalone: true,
  imports: [CommonModule, CloudinaryImageFrameComponent, PriceDisplayComponent, StarRatingComponent],
  templateUrl: './product-card.html'
})
export class ProductCardComponent {

  @Input({ required: true }) product!: ProductSummary;
  @Input() isWishlisted = false;
  @Output() selected = new EventEmitter<void>();
  @Output() addToCart = new EventEmitter<void>();
  @Output() saveToWishlist = new EventEmitter<void>();

  readonly productImageOptions: CloudinaryTransformOptions = {
    width: 560,
    height: 420,
    crop: 'pad',
    radius: 16
  };


  onSelected(): void {
    this.selected.emit();
  }

  onAddToCart(event: Event): void {
    event.stopPropagation();
    this.addToCart.emit();
  }

  onSaveToWishlist(event: Event): void {
    event.stopPropagation();
    this.saveToWishlist.emit();
  }
}
