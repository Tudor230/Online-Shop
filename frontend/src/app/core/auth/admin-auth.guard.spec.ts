import { describe, it, expect, vi, beforeEach } from 'vitest';
import { adminAuthGuard } from './admin-auth.guard';
import { AuthStateService } from './auth-state.service';
import { KeycloakAuthService } from './keycloak-auth.service';
import { Role } from './auth.types';
import { Router } from '@angular/router';
import { PLATFORM_ID } from '@angular/core';
import { createEnvironmentInjector, runInInjectionContext, EnvironmentInjector } from '@angular/core';

const mockLogin = vi.fn();
const mockNavigateByUrl = vi.fn();

vi.mock('./keycloak-auth.service', () => ({
  KeycloakAuthService: class {
    login = mockLogin;
  }
}));

describe('adminAuthGuard', () => {
  let authState: AuthStateService;
  let router: Router;
  let injector: EnvironmentInjector;

  beforeEach(() => {
    authState = new AuthStateService();
    router = { navigateByUrl: mockNavigateByUrl } as unknown as Router;
    mockLogin.mockClear();
    mockNavigateByUrl.mockClear();

    injector = createEnvironmentInjector([
      { provide: AuthStateService, useValue: authState },
      { provide: Router, useValue: router },
      { provide: PLATFORM_ID, useValue: 'browser' },
      { provide: KeycloakAuthService, useValue: { login: mockLogin } }
    ], null as any);
  });

  it('should redirect unauthenticated users to login', async () => {
    const result = await runInInjectionContext(injector, () => adminAuthGuard({} as any, {} as any));
    expect(mockLogin).toHaveBeenCalled();
    expect(result).toBe(false);
  });

  it('should allow ADMIN users', async () => {
    authState.setUser({ email: 'a@b.com', firstName: 'A', lastName: 'B', role: Role.ADMIN });
    const result = await runInInjectionContext(injector, () => adminAuthGuard({} as any, {} as any));
    expect(result).toBe(true);
  });

  it('should allow SUPPORT users', async () => {
    authState.setUser({ email: 'a@b.com', firstName: 'A', lastName: 'B', role: Role.SUPPORT });
    const result = await runInInjectionContext(injector, () => adminAuthGuard({} as any, {} as any));
    expect(result).toBe(true);
  });

  it('should redirect CUSTOMER users to home', async () => {
    authState.setUser({ email: 'a@b.com', firstName: 'A', lastName: 'B', role: Role.CUSTOMER });
    const result = await runInInjectionContext(injector, () => adminAuthGuard({} as any, {} as any));
    expect(mockNavigateByUrl).toHaveBeenCalledWith('/');
    expect(result).toBe(false);
  });

  it('should deny access during SSR (server platform)', async () => {
    const serverInjector = createEnvironmentInjector([
      { provide: AuthStateService, useValue: authState },
      { provide: Router, useValue: router },
      { provide: PLATFORM_ID, useValue: 'server' },
      { provide: KeycloakAuthService, useValue: { login: mockLogin } }
    ], null as any);

    const result = await runInInjectionContext(serverInjector, () => adminAuthGuard({} as any, {} as any));
    expect(result).toBe(false);
    expect(mockLogin).not.toHaveBeenCalled();
    expect(mockNavigateByUrl).not.toHaveBeenCalled();
  });
});
