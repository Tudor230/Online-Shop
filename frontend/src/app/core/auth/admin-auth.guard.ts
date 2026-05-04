import { isPlatformBrowser } from '@angular/common';
import { inject, PLATFORM_ID } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthStateService } from './auth-state.service';
import { KeycloakAuthService } from './keycloak-auth.service';
import { Role } from './auth.types';

export const adminAuthGuard: CanActivateFn = async () => {
  const platformId = inject(PLATFORM_ID);
  if (!isPlatformBrowser(platformId)) {
    return true;
  }

  const authState = inject(AuthStateService);
  const router = inject(Router);

  if (!authState.isAuthenticated()) {
    const keycloakAuthService = inject(KeycloakAuthService);
    await keycloakAuthService.login();
    return false;
  }

  const user = authState.user();
  const allowedRoles: Role[] = [Role.ADMIN, Role.SUPPORT];
  if (!user || !allowedRoles.includes(user.role)) {
    await router.navigateByUrl('/');
    return false;
  }

  return true;
};
