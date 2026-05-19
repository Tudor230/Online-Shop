import { test, expect } from '@playwright/test';

test.beforeEach(async ({ page }) => {
  await page.addInitScript(() => {
    (window as any).__TEST_DISABLE_KEYCLOAK = true;
  });
});

test.describe('Public Site', () => {
  test('homepage loads', async ({ page }) => {
    await page.goto('/');
    await expect(page.locator('text=Online Shop').first()).toBeVisible({ timeout: 10000 });
    await expect(page.locator('text=Crafted Tech.').first()).toBeVisible({ timeout: 10000 });
  });

  test('products page loads', async ({ page }) => {
    await page.goto('/products');
    await expect(page.getByPlaceholder('Search custom keyboards')).toBeVisible({ timeout: 10000 });
  });
});

test.describe('Admin Panel', () => {
  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => {
      (window as any).__TEST_AUTH_USER = {
        email: 'admin@test.com',
        firstName: 'Admin',
        lastName: 'User',
        role: 'ADMIN'
      };
    });
  });

  test('admin layout has sidebar navigation', async ({ page }) => {
    await page.goto('/admin');
    await expect(page.locator('aside')).toBeVisible({ timeout: 10000 });
    await expect(page.getByRole('link', { name: 'Dashboard' })).toBeVisible();
    await expect(page.getByRole('link', { name: 'Orders' })).toBeVisible();
    await expect(page.getByRole('link', { name: 'Products' })).toBeVisible();
    await expect(page.getByRole('link', { name: 'Users' })).toBeVisible();
    await expect(page.getByRole('link', { name: 'Categories' })).toBeVisible();
  });

  test('products page has search and add button', async ({ page }) => {
    await page.goto('/admin/products');
    await expect(page.getByPlaceholder('Search products...')).toBeVisible({ timeout: 10000 });
    await expect(page.getByRole('link', { name: 'Add Product' })).toBeVisible();
  });

  test('users page has search and add button', async ({ page }) => {
    await page.goto('/admin/users');
    await expect(page.getByPlaceholder('Search users...')).toBeVisible({ timeout: 10000 });
    await expect(page.getByRole('link', { name: 'Add User' })).toBeVisible();
  });

  test('orders page has status filter and search', async ({ page }) => {
    await page.goto('/admin/orders');
    await expect(page.getByRole('combobox').first()).toBeVisible({ timeout: 10000 });
    await expect(page.getByPlaceholder('Search orders...')).toBeVisible();
  });

  test('categories page has add button', async ({ page }) => {
    await page.goto('/admin/categories');
    await expect(page.getByRole('button', { name: 'Add Category' })).toBeVisible({ timeout: 10000 });
  });

  test('product form renders all fields', async ({ page }) => {
    await page.goto('/admin/products/new');
    await expect(page.getByText('New Product')).toBeVisible({ timeout: 10000 });
    await expect(page.getByLabel('SKU *')).toBeVisible();
    await expect(page.getByLabel('Name *')).toBeVisible();
    await expect(page.getByLabel('Slug *')).toBeVisible();
    await expect(page.getByRole('button', { name: 'Create' })).toBeVisible();
  });

  test('user form renders all fields', async ({ page }) => {
    await page.goto('/admin/users/new');
    await expect(page.getByText('New User')).toBeVisible({ timeout: 10000 });
    await expect(page.getByLabel('First Name *')).toBeVisible();
    await expect(page.getByLabel('Last Name *')).toBeVisible();
    await expect(page.getByRole('button', { name: 'Create' })).toBeVisible();
  });

  test('sidebar navigation changes URL', async ({ page }) => {
    await page.goto('/admin');
    await expect(page.locator('aside')).toBeVisible({ timeout: 10000 });

    await page.getByRole('link', { name: 'Products' }).click();
    await expect(page).toHaveURL(/.*\/admin\/products/);

    await page.getByRole('link', { name: 'Users' }).click();
    await expect(page).toHaveURL(/.*\/admin\/users/);

    await page.getByRole('link', { name: 'Categories' }).click();
    await expect(page).toHaveURL(/.*\/admin\/categories/);

    await page.getByRole('link', { name: 'Dashboard' }).click();
    await expect(page).toHaveURL(/.*\/admin$/);
  });
});
