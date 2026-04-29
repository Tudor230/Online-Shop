import { Inject, Injectable, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';

const GUEST_SESSION_STORAGE_KEY = 'online-shop.guest-session-id.v1';

@Injectable({ providedIn: 'root' })
export class GuestSessionService {
  private readonly isBrowser: boolean;

  constructor(@Inject(PLATFORM_ID) platformId: object) {
    this.isBrowser = isPlatformBrowser(platformId);
  }

  getOrCreateSessionId(): string | null {
    if (!this.isBrowser) {
      return null;
    }

    const existingSessionId = localStorage.getItem(GUEST_SESSION_STORAGE_KEY)?.trim();
    if (existingSessionId) {
      return existingSessionId;
    }

    const nextSessionId = this.generateSessionId();
    localStorage.setItem(GUEST_SESSION_STORAGE_KEY, nextSessionId);
    return nextSessionId;
  }

  getSessionId(): string | null {
    if (!this.isBrowser) {
      return null;
    }
    return localStorage.getItem(GUEST_SESSION_STORAGE_KEY)?.trim() || null;
  }

  clearSessionId(): void {
    if (!this.isBrowser) {
      return;
    }
    localStorage.removeItem(GUEST_SESSION_STORAGE_KEY);
  }

  private generateSessionId(): string {
    if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
      return crypto.randomUUID();
    }
    return `guest-${Math.random().toString(36).slice(2)}-${Date.now().toString(36)}`;
  }
}
