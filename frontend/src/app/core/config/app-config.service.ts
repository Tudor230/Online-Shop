import { Injectable, inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { AppConfig } from './app-config.types';

@Injectable({
  providedIn: 'root'
})
export class AppConfigService {
  private readonly http = inject(HttpClient);
  private readonly platformId = inject(PLATFORM_ID);
  private config: AppConfig | undefined;

  async loadConfig(): Promise<void> {
    if (this.config) {
      return;
    }

    if (!isPlatformBrowser(this.platformId)) {
      this.config = { cloudinaryCloudName: '' };
      return;
    }

    try {
      const configUrl = new URL('assets/config.json', document.baseURI).toString();
      const config$ = this.http.get<AppConfig>(configUrl);
      this.config = await firstValueFrom(config$);
    } catch {
      this.config = { cloudinaryCloudName: '' };
    }
  }

  get settings(): AppConfig {
    if (!this.config) {
      throw new Error('Application Configuration has not been loaded!');
    }
    return this.config;
  }
}
