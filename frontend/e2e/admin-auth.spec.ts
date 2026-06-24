import { test, expect } from '@playwright/test';
import { AdminAuthPage } from './pages/admin-auth.page';

/**
 * End-to-end tests for the admin panel using real Keycloak authentication.
 *
 * Test users (from infrastructure/keycloak-config/realm-config.json):
 * - admin / Password123;  (ADMIN role)
 * - support / Support123! (SUPPORT role)
 * - customer / (password unknown, use admin or support for these tests)
 */

test.describe('Admin Panel with Real Keycloak Auth', () => {
  test('admin user can log in and view dashboard', async ({ page }) => {
    const authPage = new AdminAuthPage(page);

    // Navigate to the app
    await authPage.gotoAdmin();

    // Log in as admin
    await authPage.login('admin', 'Password123;');

    // Should be redirected back to admin dashboard
    await expect(page).toHaveURL(/.*\/admin/);
    await expect(authPage.sidebar).toBeVisible({ timeout: 15000 });
    await expect(authPage.dashboardLink).toBeVisible();
    await expect(authPage.productsLink).toBeVisible();
    await expect(authPage.ordersLink).toBeVisible();
    await expect(authPage.usersLink).toBeVisible();
    await expect(authPage.categoriesLink).toBeVisible();
  });

  test('admin user can navigate to products page and load data', async ({ page }) => {
    const authPage = new AdminAuthPage(page);
    await authPage.gotoAdmin();

    // Keycloak login
    await authPage.login('admin', 'Password123;');

    // Wait for admin layout
    await expect(authPage.sidebar).toBeVisible({ timeout: 15000 });

    const productsResponsePromise = page.waitForResponse(
      (response) => response.url().includes('/api/admin/products') && response.request().method() === 'GET'
    );

    // Navigate to products
    await authPage.productsLink.click();
    await expect(page).toHaveURL(/.*\/admin\/products/);

    const productsResponse = await productsResponsePromise;
    expect(productsResponse.status()).toBe(200);

    // Wait for product grid or empty state to appear
    await expect(page.locator('text=Products')).toBeVisible({ timeout: 10000 });
  });

  test('admin user can navigate to users page and load data', async ({ page }) => {
    const authPage = new AdminAuthPage(page);
    await authPage.gotoAdmin();

    // Keycloak login
    await authPage.login('admin', 'Password123;');

    // Wait for admin layout
    await expect(authPage.sidebar).toBeVisible({ timeout: 15000 });

    const usersResponsePromise = page.waitForResponse(
      (response) => response.url().includes('/api/admin/users') && response.request().method() === 'GET'
    );

    // Navigate to users
    await authPage.usersLink.click();
    await expect(page).toHaveURL(/.*\/admin\/users/);

    const usersResponse = await usersResponsePromise;
    expect(usersResponse.status()).toBe(200);

    await expect(page.locator('text=Users')).toBeVisible({ timeout: 10000 });
  });

  test('support user can log in and access dashboard and orders', async ({ page }) => {
    const authPage = new AdminAuthPage(page);
    await authPage.gotoAdmin();

    // Keycloak login
    await authPage.login('support', 'Support123!');

    // Should land on admin dashboard
    await expect(authPage.sidebar).toBeVisible({ timeout: 15000 });

    // Support can access orders
    await authPage.ordersLink.click();
    await expect(page).toHaveURL(/.*\/admin\/orders/);
    await expect(page.locator('text=Orders')).toBeVisible({ timeout: 10000 });
  });

  test('support user is forbidden from users page', async ({ page }) => {
    const authPage = new AdminAuthPage(page);
    await authPage.gotoAdmin();

    // Keycloak login as support
    await authPage.login('support', 'Support123!');

    await expect(authPage.sidebar).toBeVisible({ timeout: 15000 });

    // Try to navigate to users page (admin-only)
    await page.goto('/admin/users');

    // Should be redirected away (guard redirects to home)
    await expect(page).toHaveURL('/');
  });
});
