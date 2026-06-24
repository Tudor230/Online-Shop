import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { KeycloakAuthService } from './keycloak-auth.service';
import { from, switchMap } from 'rxjs';

export const keycloakInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(KeycloakAuthService);

  // Skip adding tokens to local assets or public endpoints if needed
  if (req.url.includes('/assets/')) {
    return next(req);
  }

  // 1. Force a check. If the token expires in < 30 seconds, it refreshes it.
  // We wrap your Promise in an RxJS observable using `from()`
  return from(authService.refreshUserToken(30)).pipe(
    switchMap(() => {
      // 2. Now grab the guaranteed-fresh token
      const token = authService.getAccessToken();

      // 3. Attach it to the outgoing request
      if (token) {
        req = req.clone({
          setHeaders: {
            Authorization: `Bearer ${token}`
          }
        });
      }

      // 4. Send the request to your backend
      return next(req);
    })
  );
};
