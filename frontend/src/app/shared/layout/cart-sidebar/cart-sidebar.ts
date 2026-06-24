import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, OnChanges, OnDestroy, Output, SimpleChanges, signal } from '@angular/core';
import { CartItem } from '../../../core/cart/cart.types';
import { CloudinaryTransformOptions } from '../../../core/images/cloudinary-url.pipe';
import { CloudinaryImageFrameComponent } from '../../cloudinary-image-frame/cloudinary-image-frame';
import { PriceDisplayComponent } from '../../price-display/price-display';

@Component({
  selector: 'app-cart-sidebar',
  standalone: true,
  imports: [CommonModule, CloudinaryImageFrameComponent, PriceDisplayComponent],
  templateUrl: './cart-sidebar.html'
})
export class CartSidebarComponent implements OnChanges, OnDestroy {
  @Input({ required: true }) isOpen = false;
  @Input({ required: true }) items: CartItem[] = [];
  @Input({ required: true }) totalPrice = 0;
  @Input({ required: false }) isCheckoutInProgress = false;

  @Output() closeRequested = new EventEmitter<void>();
  @Output() incrementRequested = new EventEmitter<string>();
  @Output() decrementRequested = new EventEmitter<string>();
  @Output() removeRequested = new EventEmitter<string>();
  @Output() checkoutRequested = new EventEmitter<void>();
  @Output() itemSelected = new EventEmitter<string>();

  readonly cartImageOptions: CloudinaryTransformOptions = {
    width: 240,
    height: 180,
    crop: 'pad',
    radius: 10
  };

  readonly visible = signal(false);
  readonly isClosing = signal(false);
  private closingTimer: ReturnType<typeof setTimeout> | null = null;

  ngOnChanges(changes: SimpleChanges): void {
    if (!changes['isOpen']) return;
    const opening = changes['isOpen'].currentValue as boolean;
    if (opening) {
      if (this.closingTimer) { clearTimeout(this.closingTimer); this.closingTimer = null; }
      this.isClosing.set(false);
      this.visible.set(true);
    } else if (this.visible()) {
      this.isClosing.set(true);
      this.closingTimer = setTimeout(() => {
        this.visible.set(false);
        this.isClosing.set(false);
      }, 180);
    }
  }

  ngOnDestroy(): void {
    if (this.closingTimer) clearTimeout(this.closingTimer);
  }

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

  onCheckoutRequested(): void {
    this.checkoutRequested.emit();
  }
  onItemSelected(productSlug: string): void {
    this.itemSelected.emit(productSlug);
  }
}
