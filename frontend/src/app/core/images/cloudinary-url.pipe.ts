import { inject, Pipe, PipeTransform } from '@angular/core';
import { lazyload, responsive } from '@cloudinary/ng';
import { Cloudinary, CloudinaryImage } from '@cloudinary/url-gen';
import { AppConfigService } from '../config/app-config.service';

type CloudinaryCropMode = 'fill' | 'fit' | 'limit' | 'thumb';

export interface CloudinaryTransformOptions {
  width?: number;
  height?: number;
  crop?: CloudinaryCropMode;
  gravity?: 'auto' | 'center' | 'faces';
  radius?: number | 'max';
}

@Pipe({
  name: 'cloudinaryImage',
  standalone: true,
  pure: true,
})
export class CloudinaryImagePipe implements PipeTransform {
  private readonly appConfigService = inject(AppConfigService);
  private cloudinary: Cloudinary | null = null;

  transform(
    source: string | null | undefined,
    options: CloudinaryTransformOptions = {},
  ): CloudinaryImage | null {
    if (!source?.trim()) {
      return null;
    }

    const cloudName = this.getCloudName();
    if (!cloudName) {
      return null;
    }

    const cloudinary = this.getCloudinary(cloudName);
    const normalizedSource = source.trim();
    const image = /^https?:\/\//i.test(normalizedSource)
      ? cloudinary.image(normalizedSource).setDeliveryType('fetch').setAssetType('image')
      : cloudinary.image(normalizedSource.replace(/^\/+/, ''));

    image.addTransformation(this.buildTransformationSegment(options));
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

  private buildTransformationSegment(options: CloudinaryTransformOptions): string {
    const transforms = ['f_auto', 'q_auto', 'dpr_auto'];

    if (options.crop) {
      transforms.push(`c_${options.crop}`);
    }
    if (options.width) {
      transforms.push(`w_${options.width}`);
    }
    if (options.height) {
      transforms.push(`h_${options.height}`);
    }
    if (options.gravity) {
      transforms.push(`g_${options.gravity}`);
    }
    if (options.radius !== undefined) {
      transforms.push(`r_${options.radius}`);
    }

    return transforms.join(',');
  }
}

export const CLOUDINARY_LAZY_RESPONSIVE_PLUGINS = [lazyload(), responsive()];
export const CLOUDINARY_RESPONSIVE_PLUGINS = [responsive()];
