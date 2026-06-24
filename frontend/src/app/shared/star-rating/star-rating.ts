import { DecimalPipe } from '@angular/common';
import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-star-rating',
  standalone: true,
  imports: [DecimalPipe],
  template: `
    @if (showReviewCount && reviewCount === 0) {
      <span class="text-text-muted/75 italic">No reviews yet</span>
    } @else {
      <span class="inline-flex items-baseline gap-1">
        <span class="relative inline-block whitespace-nowrap leading-none" aria-hidden="true">
          <span class="text-text-muted/70">★★★★★</span>
          <span class="absolute inset-0 overflow-hidden whitespace-nowrap leading-none" [style.width.%]="fillPercent">
            <span class="text-[hsl(160,35%,54%)]">★★★★★</span>
          </span>
        </span>
        <span class="tabular-nums font-medium text-text-primary">{{ rating | number : '1.1-1' }}</span>
        @if (showReviewCount) {
          @if (reviewCountFormat === 'label') {
            <span class="text-text-muted/70 ml-1">{{ reviewCount }} {{ reviewCount === 1 ? 'review' : 'reviews' }}</span>
          } @else {
            <span class="text-text-muted/70">({{ reviewCount }})</span>
          }
        }
      </span>
    }
  `,
  host: { class: 'inline-flex items-baseline' }
})
export class StarRatingComponent {
  @Input({ required: true }) rating!: number;
  @Input() reviewCount = 0;
  @Input() showReviewCount = true;
  @Input() reviewCountFormat: 'parentheses' | 'label' = 'parentheses';

  get fillPercent(): number {
    return (this.rating / 5) * 100;
  }
}
