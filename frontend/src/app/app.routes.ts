import { Routes } from '@angular/router';
import { profileAuthGuard } from './core/auth/profile-auth.guard';
import { CheckoutCancelPageComponent } from './pages/checkout-cancel/checkout-cancel';
import { CheckoutPageComponent } from './pages/checkout/checkout';
import { CheckoutSuccessPageComponent } from './pages/checkout-success/checkout-success';
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
      },
      {
        path: 'checkout',
        canActivate: [profileAuthGuard],
        component: CheckoutPageComponent
      },
      {
        path: 'checkout/success',
        canActivate: [profileAuthGuard],
        component: CheckoutSuccessPageComponent
      },
      {
        path: 'checkout/cancel',
        canActivate: [profileAuthGuard],
        component: CheckoutCancelPageComponent
      }
    ]
  }
];
