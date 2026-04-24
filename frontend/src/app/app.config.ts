import { ApplicationConfig, inject, provideAppInitializer, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';

import { routes } from './app.routes';
import { provideClientHydration, withEventReplay } from '@angular/platform-browser';
import { KeycloakAuthService } from './core/auth/keycloak-auth.service';
import { authInterceptor } from './core/auth/auth.interceptor';

function initializeAuth(keycloakAuthService: KeycloakAuthService): () => Promise<void> {
  return () => keycloakAuthService.init();
}

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideHttpClient(withInterceptors([authInterceptor]), withFetch()),
    provideClientHydration(withEventReplay()),
    provideAppInitializer(() => initializeAuth(inject(KeycloakAuthService))())
  ]
};
