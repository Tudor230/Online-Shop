import { CommonModule, CurrencyPipe } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import type { Product } from '../../../assets/data/mock-products';

@Component({
  selector: 'app-product-display',
  standalone: true,
  imports: [CommonModule, CurrencyPipe],
  templateUrl: './product-display.html'
})
export class ProductDisplayComponent {
  @Input({ required: true }) isLoading = false;
  @Input({ required: true }) product: Product | null = null;
  @Input({ required: true }) selectedImage = '';
  @Input({ required: true }) selectedImageIndex = 0;

  @Output() backRequested = new EventEmitter<void>();
  @Output() imageSelected = new EventEmitter<number>();
  @Output() addToCart = new EventEmitter<void>();
  @Output() saveToWishlist = new EventEmitter<void>();

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
