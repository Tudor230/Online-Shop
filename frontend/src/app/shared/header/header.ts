import { Component, inject } from '@angular/core';
import { AuthStateService } from '../../core/auth/auth-state.service';
import { KeycloakAuthService } from '../../core/auth/keycloak-auth.service';

@Component({
  selector: 'app-header',
  standalone: true,
  templateUrl: './header.html'
})
export class HeaderComponent {
  private readonly keycloakAuthService = inject(KeycloakAuthService);
  readonly authState = inject(AuthStateService);

  async login(): Promise<void> {
    await this.keycloakAuthService.login();
  }
}

