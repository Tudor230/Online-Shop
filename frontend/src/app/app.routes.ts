import { Routes } from '@angular/router';
import { profileAuthGuard } from './core/auth/profile-auth.guard';
import { ProductDetailsComponent } from './pages/product-details/product-details';
import { ProductGridComponent } from './pages/product-grid/product-grid';
import { PrivacyPolicyPageComponent } from './pages/privacy-policy/privacy-policy';
import { ProfilePageComponent } from './pages/profile/profile';
import { ShippingReturnsPageComponent } from './pages/shipping-returns/shipping-returns';
import { TermsPageComponent } from './pages/terms/terms';
import { WarrantyPolicyPageComponent } from './pages/warranty-policy/warranty-policy';
import { CommonLayoutComponent } from './shared/layout/common-layout/common-layout';

export const routes: Routes = [
  {
    path: '',
    component: CommonLayoutComponent,
    children: [
      {
        path: '',
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
      }
    ]
  }
];
