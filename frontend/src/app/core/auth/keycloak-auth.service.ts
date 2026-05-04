import { Inject, Injectable, PLATFORM_ID } from '@angular/core';
import type Keycloak from 'keycloak-js';
import type { KeycloakInitOptions, KeycloakTokenParsed } from 'keycloak-js';
import { isPlatformBrowser } from '@angular/common';
import { keycloakConfig } from '../../keycloak.config';
import { AuthStateService } from './auth-state.service';
import { AuthUser, Role } from './auth.types';

type RoleToken = KeycloakTokenParsed & {
  email?: string;
  given_name?: string;
  family_name?: string;
  realm_access?: {
    roles?: string[];
  };
};

@Injectable({ providedIn: 'root' })
export class KeycloakAuthService {
  private keycloak: Keycloak | null = null;

  constructor(
    @Inject(PLATFORM_ID) private readonly platformId: object,
    private readonly authState: AuthStateService
  ) {}

  async init(): Promise<void> {
    if (!isPlatformBrowser(this.platformId)) {
      this.authState.clear();
      return;
    }

    // E2E testing bypass
    const testUser = (window as any).__TEST_AUTH_USER;
    if (testUser) {
      this.authState.setUser(testUser);
      return;
    }

    // Disable Keycloak in test environments where server is unavailable
    if ((window as any).__TEST_DISABLE_KEYCLOAK) {
      this.authState.clear();
      return;
    }

    const instance = await this.ensureKeycloak();

    const initOptions: KeycloakInitOptions = {
      onLoad: 'check-sso',
      checkLoginIframe: false,
      pkceMethod: 'S256'
    };

    try {
      const authenticated = await instance.init(initOptions);
      if (authenticated) {
        this.updateUserFromToken();
      } else {
        this.authState.clear();
      }
    } catch {
      // Keycloak unavailable (e.g., in E2E tests or offline)
      this.authState.clear();
    }

    instance.onAuthSuccess = () => this.updateUserFromToken();
    instance.onAuthRefreshSuccess = () => this.updateUserFromToken();
    instance.onAuthLogout = () => this.authState.clear();
    instance.onTokenExpired = () => {
      void instance
        .updateToken(30)
        .then(() => this.updateUserFromToken())
        .catch(() => this.authState.clear());
    };
  }

  async login(): Promise<void> {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }

    const instance = await this.ensureKeycloak();
    await instance.login();
  }

  async logout(): Promise<void> {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }

    const instance = await this.ensureKeycloak();
    await instance.logout({ redirectUri: window.location.origin });
    this.authState.clear();
  }

  async refreshUserToken(minValidity = 0): Promise<boolean> {
    if (!isPlatformBrowser(this.platformId)) {
      return false;
    }

    const instance = await this.ensureKeycloak();
    if (!instance.authenticated) {
      return false;
    }

    try {
      await instance.updateToken(minValidity);
      this.updateUserFromToken();
      return true;
    } catch {
      this.authState.clear();
      return false;
    }
  }

  getAccessToken(): string | null {
    return this.keycloak?.token ?? null;
  }

  private async ensureKeycloak(): Promise<Keycloak> {
    if (this.keycloak) {
      return this.keycloak;
    }

    const { default: KeycloakCtor } = await import('keycloak-js');
    this.keycloak = new KeycloakCtor({
      url: keycloakConfig.url,
      realm: keycloakConfig.realm,
      clientId: keycloakConfig.clientId
    });

    return this.keycloak;
  }

  private updateUserFromToken(): void {
    const parsedToken = this.keycloak?.tokenParsed as RoleToken | undefined;
    const user = this.extractUser(parsedToken);
    this.authState.setUser(user);
  }

  private extractUser(token?: RoleToken): AuthUser | null {
    if (!token) {
      return null;
    }

    const role = this.resolveSingleRealmRole(token.realm_access?.roles);
    if (!role) {
      return null;
    }

    return {
      email: token.email ?? '',
      firstName: token.given_name ?? '',
      lastName: token.family_name ?? '',
      role
    };
  }

  private resolveSingleRealmRole(roles?: string[]): Role | null {
    if (!roles?.length) {
      return null;
    }

    const mappedRoles = roles
      .map((roleName) => roleName.toUpperCase())
      .filter((roleName): roleName is Role => Object.values(Role).includes(roleName as Role));

    if (mappedRoles.length !== 1) {
      return null;
    }

    return mappedRoles[0];
  }
}

