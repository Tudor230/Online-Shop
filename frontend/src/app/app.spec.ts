import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of } from 'rxjs';
import { App } from './app';
import { routes } from './app.routes';
import { AuthStateService } from './core/auth/auth-state.service';
import { Role } from './core/auth/auth.types';
import { ProductApiService } from './core/products/product-api.service';

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [
        provideRouter(routes),
        {
          provide: ProductApiService,
          useValue: {
            getProducts: () => of({ items: [], page: 1, size: 25, totalItems: 0, totalPages: 0, hasPrevious: false, hasNext: false }),
            getCategories: () => of([])
          }
        }
      ]
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('should render login button when unauthenticated', async () => {
    const fixture = TestBed.createComponent(App);
    const router = TestBed.inject(Router);

    await router.navigateByUrl('/products');
    fixture.detectChanges();
    await fixture.whenStable();

    const compiled = fixture.nativeElement as HTMLElement;
    const loginButton = Array.from(compiled.querySelectorAll('button')).find(
      (button) => button.textContent?.trim() === 'Sign in'
    );

    expect(loginButton).toBeTruthy();
  });

  it('should render full user name when authenticated', async () => {
    const fixture = TestBed.createComponent(App);
    const authState = TestBed.inject(AuthStateService);
    const router = TestBed.inject(Router);

    authState.setUser({
      email: 'john.doe@example.com',
      firstName: 'John',
      lastName: 'Doe',
      role: Role.CUSTOMER
    });

    await router.navigateByUrl('/products');
    fixture.detectChanges();
    await fixture.whenStable();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('John Doe');
    expect(compiled.textContent).not.toContain('Login');
  });

  it('should render the product grid heading', async () => {
    const fixture = TestBed.createComponent(App);
    const router = TestBed.inject(Router);
    await router.navigateByUrl('/products');
    fixture.detectChanges();
    await fixture.whenStable();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Gaming Tech Grid');
  });
});
