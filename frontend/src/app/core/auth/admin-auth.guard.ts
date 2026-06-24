import { isPlatformBrowser } from '@angular/common';
import { inject, isDevMode, PLATFORM_ID } from '@angular/core';
import { CanActivateChildFn, CanActivateFn, Router } from '@angular/router';
import { AuthStateService } from './auth-state.service';
import { KeycloakAuthService } from './keycloak-auth.service';
import { Role } from './auth.types';

export const adminAuthGuard: CanActivateFn = async () => {
  const platformId = inject(PLATFORM_ID);
  if (!isPlatformBrowser(platformId)) {
    // Allow SSR to render the route shell; client-side guard will re-evaluate and redirect if needed
    return true;
  }

  const authState = inject(AuthStateService);
  const router = inject(Router);

  if (isDevMode()) {
    // E2E testing bypass: allow tests to inject auth user via window global
    const testUser = (window as any).__TEST_AUTH_USER;
    if (testUser && !authState.isAuthenticated()) {
      authState.setUser(testUser);
    }
  }

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

export const adminChildAuthGuard: CanActivateChildFn = (childRoute) => {
  const authState = inject(AuthStateService);
  const router = inject(Router);
  const user = authState.user();
  const requiredRoles = childRoute.data?.['roles'] as Role[] | undefined;

  if (!user) {
    router.navigateByUrl('/');
    return false;
  }

  if (requiredRoles && !requiredRoles.includes(user.role)) {
    router.navigateByUrl('/');
    return false;
  }

  return true;
};
