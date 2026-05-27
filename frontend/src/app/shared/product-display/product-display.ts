import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common';
import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { CreateProductReviewRequest, ProductDetails } from '../../core/products/product.types';
import { CloudinaryTransformOptions } from '../../core/images/cloudinary-url.pipe';
import { CloudinaryImageFrameComponent } from '../cloudinary-image-frame/cloudinary-image-frame';

@Component({
  selector: 'app-product-display',
  standalone: true,
  imports: [CommonModule, CurrencyPipe, CloudinaryImageFrameComponent, DatePipe, FormsModule, RouterLink],
  templateUrl: './product-display.html'
})
export class ProductDisplayComponent implements OnChanges {
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

  @Output() backRequested = new EventEmitter<void>();
  @Output() imageSelected = new EventEmitter<number>();
  @Output() previousImageRequested = new EventEmitter<void>();
  @Output() nextImageRequested = new EventEmitter<void>();
  @Output() addToCart = new EventEmitter<void>();
  @Output() reviewSubmitted = new EventEmitter<CreateProductReviewRequest>();
  @Output() saveToWishlist = new EventEmitter<void>();

  readonly heroImageOptions: CloudinaryTransformOptions = {
    width: 1200,
    height: 900,
    crop: 'pad',
    radius: 24
  };

  readonly thumbnailImageOptions: CloudinaryTransformOptions = {
    width: 240,
    height: 180,
    crop: 'pad',
    radius: 12
  };


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

  ngOnChanges(changes: SimpleChanges): void {
    const reviewFormResetTokenChange = changes['reviewFormResetToken'];
    if (!reviewFormResetTokenChange || reviewFormResetTokenChange.firstChange) {
      return;
    }

    this.reviewComment = '';
    this.reviewRating = 5;
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
