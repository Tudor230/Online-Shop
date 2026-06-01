import { inject, Pipe, PipeTransform } from '@angular/core';
import { lazyload, responsive } from '@cloudinary/ng';
import { Cloudinary, CloudinaryImage } from '@cloudinary/url-gen';
import { AppConfigService } from '../config/app-config.service';

type CloudinaryCropMode = 'fill' | 'fit' | 'limit' | 'thumb' | 'pad';

export interface CloudinaryTransformOptions {
  width?: number;
  height?: number;
  crop?: CloudinaryCropMode;
  gravity?: 'auto' | 'center' | 'faces';
  radius?: number | 'max';
  backgroundColor?: string;
  shadowStrength?: number;
  removeBackground?: boolean;
}

@Pipe({
  name: 'cloudinaryImage',
  standalone: true,
  pure: true
})
export class CloudinaryImagePipe implements PipeTransform {
  private readonly appConfigService = inject(AppConfigService);
  private cloudinary: Cloudinary | null = null;

  transform(source: string | null | undefined, options: CloudinaryTransformOptions = {}): CloudinaryImage | null {
    if (!source?.trim()) {
      return null;
    }

    const cloudName = this.getCloudName();
    if (!cloudName) {
      return null;
    }

    const cloudinary = this.getCloudinary(cloudName);
    const normalizedSource = source.trim();
    const isFetchSource = /^https?:\/\//i.test(normalizedSource);
    const image = isFetchSource
      ? cloudinary.image(normalizedSource).setDeliveryType('fetch').setAssetType('image')
      : cloudinary.image(normalizedSource.replace(/^\/+/, ''));

    image.addTransformation(this.buildTransformationSegment(options, isFetchSource));
    return image;
  }

  private getCloudName(): string | null {
    try {
      return this.appConfigService.settings.cloudinaryCloudName?.trim() || null;
    } catch {
      return null;
    }
  }

  private getCloudinary(cloudName: string): Cloudinary {
    if (!this.cloudinary) {
      this.cloudinary = new Cloudinary({ cloud: { cloudName } });
    }

    return this.cloudinary;
  }

  private buildTransformationSegment(options: CloudinaryTransformOptions, isFetchSource: boolean): string {
    const components = ['f_auto,q_auto,dpr_auto'];
    const resizeQualifiers: string[] = [];
    const hasBounds = Boolean(options.width || options.height);
    const cropMode = options.crop ?? (hasBounds ? 'pad' : undefined);
    const gravity = options.gravity;
    const backgroundColor = this.normalizeColor(options.backgroundColor ?? 'f2efe9');
    const shadowStrength = options.shadowStrength ?? 36;
    const removeBackground = options.removeBackground ?? true;

    if (cropMode) {
      resizeQualifiers.push(`c_${cropMode}`);
    }
    if (options.width) {
      resizeQualifiers.push(`w_${options.width}`);
    }
    if (options.height) {
      resizeQualifiers.push(`h_${options.height}`);
    }
    if (gravity) {
      resizeQualifiers.push(`g_${gravity}`);
    }
    if (hasBounds && cropMode === 'pad') {
      resizeQualifiers.push(this.backgroundQualifier(backgroundColor));
    }

    if (resizeQualifiers.length > 0) {
      components.push(resizeQualifiers.join(','));
    }

    // Cloudinary docs: both e_background_removal and e_dropshadow are unsupported for fetched images.
    // Also, e_dropshadow works best after transparency is present, so chain it after background removal.
    if (!isFetchSource && removeBackground) {
      components.push('e_background_removal');
    }

    if (!isFetchSource && shadowStrength > 0) {
      components.push(this.dropShadowEffect(shadowStrength));
    }

    if (options.radius !== undefined) {
      components.push(`r_${options.radius}`);
    }

    return components.join('/');
  }

  private normalizeColor(color: string): string {
    const normalizedColor = color.trim().replace(/^#/, '').toLowerCase();
    return normalizedColor || 'f2efe9';
  }

  private backgroundQualifier(color: string): string {
    return /^[0-9a-f]{3}([0-9a-f]{3})?$/i.test(color) ? `b_rgb:${color}` : `b_${color}`;
  }

  private dropShadowEffect(shadowStrength: number): string {
    const spread = this.clamp(Math.round(shadowStrength), 0, 100);
    return `e_dropshadow:azimuth_220;elevation_45;spread_${spread}`;
  }

  private clamp(value: number, min: number, max: number): number {
    return Math.min(max, Math.max(min, value));
  }
}

export const CLOUDINARY_LAZY_RESPONSIVE_PLUGINS = [lazyload(), responsive()];
export const CLOUDINARY_RESPONSIVE_PLUGINS = [responsive()];

