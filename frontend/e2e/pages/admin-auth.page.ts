import { type Locator, type Page } from '@playwright/test';

export class AdminAuthPage {
  readonly page: Page;
  readonly usernameInput: Locator;
  readonly passwordInput: Locator;
  readonly loginButton: Locator;
  readonly sidebar: Locator;
  readonly dashboardLink: Locator;
  readonly productsLink: Locator;
  readonly ordersLink: Locator;
  readonly usersLink: Locator;
  readonly categoriesLink: Locator;

  constructor(page: Page) {
    this.page = page;
    this.usernameInput = page.locator('input#username');
    this.passwordInput = page.locator('input#password');
    this.loginButton = page.locator('#kc-login');
    this.sidebar = page.locator('aside');
    this.dashboardLink = page.getByRole('link', { name: 'Dashboard' });
    this.productsLink = page.getByRole('link', { name: 'Products' });
    this.ordersLink = page.getByRole('link', { name: 'Orders' });
    this.usersLink = page.getByRole('link', { name: 'Users' });
    this.categoriesLink = page.getByRole('link', { name: 'Categories' });
  }

  async gotoAdmin(): Promise<void> {
    await this.page.goto('http://localhost:4200/admin');
  }

  async login(username: string, password: string): Promise<void> {
    await this.usernameInput.waitFor({ state: 'visible', timeout: 10000 });
    await this.passwordInput.waitFor({ state: 'visible', timeout: 10000 });
    await this.usernameInput.fill(username);
    await this.passwordInput.fill(password);
    await this.loginButton.click();
  }
}
