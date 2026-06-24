import { isPlatformBrowser } from '@angular/common';
import { inject, PLATFORM_ID } from '@angular/core';
import { CanActivateFn } from '@angular/router';
import { AuthStateService } from './auth-state.service';
import { KeycloakAuthService } from './keycloak-auth.service';

export const profileAuthGuard: CanActivateFn = async () => {
  const platformId = inject(PLATFORM_ID);
  if (!isPlatformBrowser(platformId)) {
    return true;
  }

  const authState = inject(AuthStateService);
  if (authState.isAuthenticated()) {
    return true;
  }

  const keycloakAuthService = inject(KeycloakAuthService);
  await keycloakAuthService.login();
  return false;
};
