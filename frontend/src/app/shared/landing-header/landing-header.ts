import { Component, HostListener, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthStateService } from '../../core/auth/auth-state.service';
import { KeycloakAuthService } from '../../core/auth/keycloak-auth.service';

@Component({
  selector: 'app-landing-header',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './landing-header.html',
})
export class LandingHeaderComponent {
  private readonly keycloakAuthService = inject(KeycloakAuthService);
  readonly authState = inject(AuthStateService);

  readonly isHidden = signal<boolean>(true);
  private lastScrollY = 0;

  @HostListener('window:scroll')
  onWindowScroll(): void {
    const currentScrollY = window.scrollY;
    const firstSectionPx = window.innerHeight * 2.8;

    if (currentScrollY < firstSectionPx) {
      this.isHidden.set(true);
      this.lastScrollY = currentScrollY;
      return;
    }

    const scrollDelta = currentScrollY - this.lastScrollY;
    if (scrollDelta > 0) {
      this.isHidden.set(true);
    } else if (scrollDelta < 0) {
      this.isHidden.set(false);
    }

    this.lastScrollY = currentScrollY;
  }

  async login(): Promise<void> {
    await this.keycloakAuthService.login();
  }
}
