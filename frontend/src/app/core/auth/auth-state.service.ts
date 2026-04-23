import { computed, Injectable, signal } from '@angular/core';
import { AuthUser } from './auth.types';

@Injectable({ providedIn: 'root' })
export class AuthStateService {
  private readonly userState = signal<AuthUser | null>(null);

  readonly user = this.userState.asReadonly();
  readonly isAuthenticated = computed(() => this.user() !== null);
  readonly displayName = computed(() => {
    const user = this.user();
    if (!user) {
      return '';
    }

    const fullName = `${user.firstName} ${user.lastName}`.trim();
    if (fullName) {
      return fullName;
    }

    const emailPrefix = user.email.split('@')[0]?.trim();
    return emailPrefix || 'User';
  });

  setUser(user: AuthUser | null): void {
    this.userState.set(user);
  }

  clear(): void {
    this.userState.set(null);
  }
}

