import { ApplicationConfig, inject, provideAppInitializer, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';
import { provideClientHydration, withEventReplay } from '@angular/platform-browser';
import { KeycloakAuthService } from './core/auth/keycloak-auth.service';

function initializeAuth(keycloakAuthService: KeycloakAuthService): () => Promise<void> {
  return () => keycloakAuthService.init();
}

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideClientHydration(withEventReplay()),
    provideAppInitializer(() => initializeAuth(inject(KeycloakAuthService))())
  ]
};
