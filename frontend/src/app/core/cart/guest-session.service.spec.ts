import { PLATFORM_ID } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { GuestSessionService } from './guest-session.service';

const STORAGE_KEY = 'online-shop.guest-session-id.v1';

describe('GuestSessionService', () => {
  beforeEach(() => {
    localStorage.removeItem(STORAGE_KEY);
    TestBed.configureTestingModule({
      providers: [{ provide: PLATFORM_ID, useValue: 'browser' }],
    });
  });

  it('should create and persist a guest session id', () => {
    const service = TestBed.inject(GuestSessionService);
    const sessionId = service.getOrCreateSessionId();

    expect(sessionId).not.toBeNull();
    expect(sessionId?.length ?? 0).toBeGreaterThan(0);
    expect(localStorage.getItem(STORAGE_KEY)).toBe(sessionId);
  });

  it('should reuse an existing guest session id', () => {
    localStorage.setItem(STORAGE_KEY, 'guest-existing-session');
    const service = TestBed.inject(GuestSessionService);

    expect(service.getOrCreateSessionId()).toBe('guest-existing-session');
  });

  it('should return null when session id is missing', () => {
    const service = TestBed.inject(GuestSessionService);

    expect(service.getSessionId()).toBeNull();
  });

  it('should clear the stored session id', () => {
    localStorage.setItem(STORAGE_KEY, 'guest-existing-session');
    const service = TestBed.inject(GuestSessionService);

    service.clearSessionId();

    expect(service.getSessionId()).toBeNull();
  });
});
