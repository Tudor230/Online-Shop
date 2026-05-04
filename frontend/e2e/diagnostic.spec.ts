import { test, expect } from '@playwright/test';

test.beforeEach(async ({ page }) => {
  await page.addInitScript(() => {
    (window as any).__TEST_DISABLE_KEYCLOAK = true;
  });
});

test('public homepage loads', async ({ page }) => {
  await page.goto('http://127.0.0.1:8080/');
  await page.waitForTimeout(3000);

  const title = await page.title();
  console.log('Title:', title);
  expect(title).toBe('Online Shop');

  // Check that Angular bootstrapped
  const appRoot = page.locator('app-root');
  await expect(appRoot).toBeAttached();
});

test('admin page loads with auth mock', async ({ page }) => {
  await page.addInitScript(() => {
    (window as any).__TEST_AUTH_USER = {
      email: 'admin@test.com',
      firstName: 'Admin',
      lastName: 'User',
      role: 'ADMIN'
    };
  });

  await page.goto('http://127.0.0.1:8080/admin');
  await page.waitForTimeout(3000);

  const title = await page.title();
  console.log('Admin title:', title);
  expect(title).toBe('Online Shop');

  // Check sidebar rendered
  await expect(page.locator('aside')).toBeVisible();
  await expect(page.getByRole('link', { name: 'Dashboard' })).toBeVisible();
});
