import { Routes } from '@angular/router';
import { profileAuthGuard } from './core/auth/profile-auth.guard';
import { adminAuthGuard } from './core/auth/admin-auth.guard';
import { ProductDetailsComponent } from './pages/product-details/product-details';
import { ProductGridComponent } from './pages/product-grid/product-grid';
import { WelcomeComponent } from './pages/welcome/welcome';
import { PrivacyPolicyPageComponent } from './pages/privacy-policy/privacy-policy';
import { ProfilePageComponent } from './pages/profile/profile';
import { ShippingReturnsPageComponent } from './pages/shipping-returns/shipping-returns';
import { TermsPageComponent } from './pages/terms/terms';
import { WarrantyPolicyPageComponent } from './pages/warranty-policy/warranty-policy';
import { CommonLayoutComponent } from './shared/layout/common-layout/common-layout';
import { OrderHistoryPageComponent } from './pages/order-history/order-history';

export const routes: Routes = [
  {
    path: '',
    component: WelcomeComponent
  },
  {
    path: '',
    component: CommonLayoutComponent,
    children: [
      {
        path: 'products',
        component: ProductGridComponent
      },
      {
        path: 'product/:id',
        component: ProductDetailsComponent
      },
      {
        path: 'profile',
        canActivate: [profileAuthGuard],
        component: ProfilePageComponent
      },
      {
        path: 'shipping-and-returns',
        component: ShippingReturnsPageComponent
      },
      {
        path: 'warranty-policy',
        component: WarrantyPolicyPageComponent
      },
      {
        path: 'privacy-policy',
        component: PrivacyPolicyPageComponent
      },
      {
        path: 'terms',
        component: TermsPageComponent
      },
      {
        path: 'orders',
        canActivate: [profileAuthGuard],
        component: OrderHistoryPageComponent
      }
    ]
  },
  {
    path: 'admin',
    canActivate: [adminAuthGuard],
    loadComponent: () => import('./pages/admin/admin-layout').then(m => m.AdminLayoutComponent),
    children: [
      { path: '', loadComponent: () => import('./pages/admin/dashboard/dashboard').then(m => m.AdminDashboardComponent) },
      { path: 'users', loadComponent: () => import('./pages/admin/users/users').then(m => m.AdminUsersComponent) },
      { path: 'users/new', loadComponent: () => import('./pages/admin/users/user-form').then(m => m.AdminUserFormComponent) },
      { path: 'users/:id/edit', loadComponent: () => import('./pages/admin/users/user-form').then(m => m.AdminUserFormComponent) },
      { path: 'products', loadComponent: () => import('./pages/admin/products/products').then(m => m.AdminProductsComponent) },
      { path: 'products/new', loadComponent: () => import('./pages/admin/products/product-form').then(m => m.AdminProductFormComponent) },
      { path: 'products/:id/edit', loadComponent: () => import('./pages/admin/products/product-form').then(m => m.AdminProductFormComponent) },
      { path: 'orders', loadComponent: () => import('./pages/admin/orders/orders').then(m => m.AdminOrdersComponent) },
      { path: 'categories', loadComponent: () => import('./pages/admin/categories/categories').then(m => m.AdminCategoriesComponent) },
      { path: 'audit-logs', loadComponent: () => import('./pages/admin/audit-logs/audit-logs').then(m => m.AdminAuditLogsComponent) }
    ]
  }
];
