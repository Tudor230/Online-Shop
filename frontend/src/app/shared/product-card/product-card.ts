import { CommonModule, CurrencyPipe } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import type { Product } from '../../../assets/data/mock-products';

@Component({
  selector: 'app-product-card',
  standalone: true,
  imports: [CommonModule, CurrencyPipe],
  templateUrl: './product-card.html'
})
export class ProductCardComponent {
  @Input({ required: true }) product!: Product;
  @Output() selected = new EventEmitter<void>();
  @Output() addToCart = new EventEmitter<void>();
  @Output() saveToWishlist = new EventEmitter<void>();

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
