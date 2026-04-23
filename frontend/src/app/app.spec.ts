import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { App } from './app';
import { routes } from './app.routes';
import { AuthStateService } from './core/auth/auth-state.service';
import { Role } from './core/auth/auth.types';

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideRouter(routes)]
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('should render login button when unauthenticated', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    const loginButton = compiled.querySelector('button');

    expect(loginButton?.textContent?.trim()).toBe('Login');
  });

  it('should render full user name when authenticated', () => {
    const fixture = TestBed.createComponent(App);
    const authState = TestBed.inject(AuthStateService);

    authState.setUser({
      email: 'john.doe@example.com',
      firstName: 'John',
      lastName: 'Doe',
      role: Role.CUSTOMER
    });

    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('John Doe');
    expect(compiled.textContent).not.toContain('Login');
  });

  it('should render the product grid heading', async () => {
    const fixture = TestBed.createComponent(App);
    const router = TestBed.inject(Router);
    await router.navigateByUrl('/');
    fixture.detectChanges();
    await fixture.whenStable();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Gaming Tech Grid');
  });
});
