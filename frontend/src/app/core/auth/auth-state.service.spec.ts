import { describe, it, expect, beforeEach } from 'vitest';
import { AuthStateService } from './auth-state.service';
import { Role } from './auth.types';

describe('AuthStateService', () => {
  let service: AuthStateService;

  beforeEach(() => {
    service = new AuthStateService();
  });

  it('should be unauthenticated by default', () => {
    expect(service.isAuthenticated()).toBe(false);
    expect(service.isAdmin()).toBe(false);
    expect(service.displayName()).toBe('');
  });

  it('should identify ADMIN as admin', () => {
    service.setUser({ email: 'a@b.com', firstName: 'A', lastName: 'B', role: Role.ADMIN });
    expect(service.isAdmin()).toBe(true);
  });

  it('should identify SUPPORT as admin', () => {
    service.setUser({ email: 'a@b.com', firstName: 'A', lastName: 'B', role: Role.SUPPORT });
    expect(service.isAdmin()).toBe(true);
  });

  it('should not identify CUSTOMER as admin', () => {
    service.setUser({ email: 'a@b.com', firstName: 'A', lastName: 'B', role: Role.CUSTOMER });
    expect(service.isAdmin()).toBe(false);
  });

  it('should compute display name from full name', () => {
    service.setUser({ email: 'a@b.com', firstName: 'John', lastName: 'Doe', role: Role.CUSTOMER });
    expect(service.displayName()).toBe('John Doe');
  });

  it('should compute display name from email prefix when name is empty', () => {
    service.setUser({ email: 'john@example.com', firstName: '', lastName: '', role: Role.CUSTOMER });
    expect(service.displayName()).toBe('john');
  });

  it('should clear user on logout', () => {
    service.setUser({ email: 'a@b.com', firstName: 'A', lastName: 'B', role: Role.ADMIN });
    service.clear();
    expect(service.isAuthenticated()).toBe(false);
    expect(service.isAdmin()).toBe(false);
  });
});
