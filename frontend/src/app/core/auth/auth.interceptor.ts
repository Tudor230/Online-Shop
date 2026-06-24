import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { KeycloakAuthService } from './keycloak-auth.service';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const keycloakAuthService = inject(KeycloakAuthService);
  const token = keycloakAuthService.getAccessToken();

  if (!token) {
    return next(request);
  }

  return next(
    request.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    })
  );
};
