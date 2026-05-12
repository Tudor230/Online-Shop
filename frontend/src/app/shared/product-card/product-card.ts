import { CommonModule, CurrencyPipe } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { ProductSummary } from '../../core/products/product.types';
import { CloudinaryTransformOptions } from '../../core/images/cloudinary-url.pipe';
import { CloudinaryImageFrameComponent } from '../cloudinary-image-frame/cloudinary-image-frame';

@Component({
  selector: 'app-product-card',
  standalone: true,
  imports: [CommonModule, CurrencyPipe, CloudinaryImageFrameComponent],
  templateUrl: './product-card.html'
})
export class ProductCardComponent {

  @Input({ required: true }) product!: ProductSummary;
  @Output() selected = new EventEmitter<void>();
  @Output() addToCart = new EventEmitter<void>();
  @Output() saveToWishlist = new EventEmitter<void>();

  readonly productImageOptions: CloudinaryTransformOptions = {
    width: 560,
    height: 420,
    crop: 'limit',
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
