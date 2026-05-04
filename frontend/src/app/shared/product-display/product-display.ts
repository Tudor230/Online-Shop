import { CommonModule, CurrencyPipe } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { ProductDetails } from '../../core/products/product.types';
import { CloudinaryTransformOptions } from '../../core/images/cloudinary-url.pipe';
import { CloudinaryImageFrameComponent } from '../cloudinary-image-frame/cloudinary-image-frame';

@Component({
  selector: 'app-product-display',
  standalone: true,
  imports: [CommonModule, CurrencyPipe, CloudinaryImageFrameComponent],
  templateUrl: './product-display.html'
})
export class ProductDisplayComponent {
  @Input({ required: true }) isLoading = false;
  @Input({ required: true }) product: ProductDetails | null = null;
  @Input({ required: true }) selectedImageId = '';
  @Input({ required: true }) selectedImageIndex = 0;
  @Input({ required: true }) isWishlisted = false;

  @Output() backRequested = new EventEmitter<void>();
  @Output() imageSelected = new EventEmitter<number>();
  @Output() addToCart = new EventEmitter<void>();
  @Output() saveToWishlist = new EventEmitter<void>();

  readonly heroImageOptions: CloudinaryTransformOptions = {
    width: 1200,
    height: 960,
    crop: 'limit',
    radius: 24
  };

  readonly thumbnailImageOptions: CloudinaryTransformOptions = {
    width: 240,
    height: 180,
    crop: 'limit',
    radius: 12
  };


  onBackRequested(): void {
    this.backRequested.emit();
  }

  onImageSelected(index: number): void {
    this.imageSelected.emit(index);
  }

  onAddToCart(): void {
    this.addToCart.emit();
  }

  onSaveToWishlist(): void {
    this.saveToWishlist.emit();
  }
}
