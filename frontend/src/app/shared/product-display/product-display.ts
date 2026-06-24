import { CommonModule } from '@angular/common';
import { Component, ElementRef, EventEmitter, Input, OnChanges, Output, SimpleChanges, ViewChild, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { CreateProductReviewRequest, ProductDetails } from '../../core/products/product.types';
import { CloudinaryTransformOptions } from '../../core/images/cloudinary-url.pipe';
import { CloudinaryImageFrameComponent } from '../cloudinary-image-frame/cloudinary-image-frame';
import { PriceDisplayComponent } from '../price-display/price-display';
import { StarRatingComponent } from '../star-rating/star-rating';

@Component({
  selector: 'app-product-display',
  standalone: true,
  imports: [CommonModule, CloudinaryImageFrameComponent, FormsModule, RouterLink, PriceDisplayComponent, StarRatingComponent],
  templateUrl: './product-display.html'
})
export class ProductDisplayComponent implements OnChanges {
  @ViewChild('lightboxEl') private lightboxEl?: ElementRef<HTMLElement>;

  @Input({ required: true }) isLoading = false;
  @Input({ required: true }) product: ProductDetails | null = null;
  @Input({ required: true }) isAuthenticated = false;
  @Input({ required: true }) isSubmittingReview = false;
  @Input({ required: true }) reviewError: string | null = null;
  @Input({ required: true }) selectedImageId = '';
  @Input({ required: true }) selectedImageIndex = 0;
  @Input({ required: true }) isWishlisted = false;
  @Input({ required: true }) reviewFormResetToken = 0;

  reviewRating = 5;
  reviewComment = '';

  readonly lightboxOpen = signal(false);
  readonly zoomLevel = signal(1);
  private panX = 0;
  private panY = 0;
  private isDragging = false;
  private dragStartX = 0;
  private dragStartY = 0;

  @Output() backRequested = new EventEmitter<void>();
  @Output() imageSelected = new EventEmitter<number>();
  @Output() previousImageRequested = new EventEmitter<void>();
  @Output() nextImageRequested = new EventEmitter<void>();
  @Output() addToCart = new EventEmitter<void>();
  @Output() reviewSubmitted = new EventEmitter<CreateProductReviewRequest>();
  @Output() saveToWishlist = new EventEmitter<void>();
  @Output() lightboxStateChanged = new EventEmitter<boolean>();

  readonly heroImageOptions: CloudinaryTransformOptions = {
    width: 1200,
    height: 900,
    crop: 'pad',
    radius: 24
  };

  readonly lightboxImageOptions: CloudinaryTransformOptions = {
    width: 2400,
    height: 1800,
    crop: 'pad'
  };

  readonly thumbnailImageOptions: CloudinaryTransformOptions = {
    width: 240,
    height: 180,
    crop: 'pad',
    radius: 12
  };

  get lightboxTransform(): string {
    return `scale(${this.zoomLevel()}) translate(${this.panX}px, ${this.panY}px)`;
  }

  ngOnChanges(changes: SimpleChanges): void {
    const reviewFormResetTokenChange = changes['reviewFormResetToken'];
    if (!reviewFormResetTokenChange || reviewFormResetTokenChange.firstChange) {
      return;
    }

    this.reviewComment = '';
    this.reviewRating = 5;
  }

  openLightbox(): void {
    this.zoomLevel.set(1);
    this.panX = 0;
    this.panY = 0;
    this.lightboxOpen.set(true);
    this.lightboxStateChanged.emit(true);
    queueMicrotask(() => this.lightboxEl?.nativeElement?.focus());
  }

  closeLightbox(): void {
    this.lightboxOpen.set(false);
    this.lightboxStateChanged.emit(false);
  }

  onLightboxKeydown(event: KeyboardEvent): void {
    if (event.key === 'Escape') {
      this.closeLightbox();
    }
  }

  onLightboxWheel(event: WheelEvent): void {
    event.preventDefault();
    const delta = event.deltaY > 0 ? -0.15 : 0.15;
    const next = Math.max(0.5, Math.min(5, this.zoomLevel() + delta));
    this.zoomLevel.set(next);
    if (next <= 1) {
      this.panX = 0;
      this.panY = 0;
    }
  }

  onLightboxMouseDown(event: MouseEvent): void {
    event.preventDefault();
    if (this.zoomLevel() <= 1) return;
    this.isDragging = true;
    this.dragStartX = event.clientX - this.panX;
    this.dragStartY = event.clientY - this.panY;
  }

  onLightboxMouseMove(event: MouseEvent): void {
    if (!this.isDragging) return;
    this.panX = event.clientX - this.dragStartX;
    this.panY = event.clientY - this.dragStartY;
  }

  onLightboxMouseUp(): void {
    this.isDragging = false;
  }

  onBackRequested(): void {
    this.backRequested.emit();
  }

  onImageSelected(index: number): void {
    this.imageSelected.emit(index);
  }

  onPreviousImageRequested(): void {
    this.previousImageRequested.emit();
  }

  onNextImageRequested(): void {
    this.nextImageRequested.emit();
  }

  onAddToCart(): void {
    this.addToCart.emit();
  }

  onSubmitReview(): void {
    const trimmedComment = this.reviewComment.trim();
    if (!trimmedComment) {
      return;
    }

    this.reviewSubmitted.emit({
      rating: this.reviewRating,
      comment: trimmedComment
    });
  }

  onSaveToWishlist(): void {
    this.saveToWishlist.emit();
  }
}
