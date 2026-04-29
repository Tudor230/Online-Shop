import { CommonModule, CurrencyPipe } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CartItem } from '../../../core/cart/cart.types';

@Component({
  selector: 'app-cart-sidebar',
  standalone: true,
  imports: [CommonModule, CurrencyPipe],
  templateUrl: './cart-sidebar.html'
})
export class CartSidebarComponent {
  @Input({ required: true }) isOpen = false;
  @Input({ required: true }) items: CartItem[] = [];
  @Input({ required: true }) totalPrice = 0;

  @Output() closeRequested = new EventEmitter<void>();
  @Output() incrementRequested = new EventEmitter<string>();
  @Output() decrementRequested = new EventEmitter<string>();
  @Output() removeRequested = new EventEmitter<string>();

  onCloseRequested(): void {
    this.closeRequested.emit();
  }

  onIncrementRequested(productId: string): void {
    this.incrementRequested.emit(productId);
  }

  onDecrementRequested(productId: string): void {
    this.decrementRequested.emit(productId);
  }

  onRemoveRequested(productId: string): void {
    this.removeRequested.emit(productId);
  }
}
