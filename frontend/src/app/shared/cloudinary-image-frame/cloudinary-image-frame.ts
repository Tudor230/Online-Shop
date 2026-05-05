import { CommonModule, isPlatformBrowser } from '@angular/common';
import { Component, Input, PLATFORM_ID, inject, OnChanges, SimpleChanges } from '@angular/core';
import { CloudinaryModule } from '@cloudinary/ng';
import { CloudinaryImagePipe, CloudinaryTransformOptions, CLOUDINARY_LAZY_RESPONSIVE_PLUGINS, CLOUDINARY_RESPONSIVE_PLUGINS } from '../../core/images/cloudinary-url.pipe';

@Component({
  selector: 'app-cloudinary-image-frame',
  standalone: true,
  imports: [CommonModule, CloudinaryModule, CloudinaryImagePipe],
  host: { class: 'block h-full w-full' },
  template: `
    <ng-template #fallback>
      <div [class]="fallbackClass">{{ fallbackText }}</div>
    </ng-template>

    @if (!imageUnavailable) {
      @if (source | cloudinaryImage: options; as cloudinaryImage) {
        <advanced-image
          [cldImg]="cloudinaryImage"
          [plugins]="resolvedPlugins"
          [alt]="alt"
          [attr.width]="width"
          [attr.height]="height"
          [attr.loading]="loading"
          [class]="imageClass"
          (error)="onImageError()"
        />
      } @else {
        <ng-container [ngTemplateOutlet]="fallback" />
      }
    } @else {
      <ng-container [ngTemplateOutlet]="fallback" />
    }
  `
})
export class CloudinaryImageFrameComponent implements OnChanges {
  private readonly platformId = inject(PLATFORM_ID);
  imageUnavailable = false;

  @Input({ required: true }) source = '';
  @Input({ required: true }) alt = '';
  @Input() options: CloudinaryTransformOptions = {};
  @Input() width: number | string | null = null;
  @Input() height: number | string | null = null;
  @Input() loading: 'eager' | 'lazy' | null = 'lazy';
  @Input() imageClass = 'h-full w-full object-contain';
  @Input() fallbackClass = 'grid h-full w-full place-items-center px-3 text-center text-sm font-medium text-text-secondary';
  @Input() fallbackText = 'Image unavailable';
  @Input() useResponsive = false;
  @Input() useLazy = true;

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['source']) {
      this.imageUnavailable = false;
    }
  }

  get resolvedPlugins() {
    if (!isPlatformBrowser(this.platformId)) {
      return [];
    }

    if (this.useResponsive) {
      return CLOUDINARY_RESPONSIVE_PLUGINS;
    }

    if (this.useLazy) {
      return CLOUDINARY_LAZY_RESPONSIVE_PLUGINS;
    }

    return [];
  }

  onImageError(): void {
    this.imageUnavailable = true;
  }
}


