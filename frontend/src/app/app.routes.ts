import { Routes } from '@angular/router';
import { profileAuthGuard } from './core/auth/profile-auth.guard';
import { ProductDetailsComponent } from './pages/product-details/product-details';
import { ProductGridComponent } from './pages/product-grid/product-grid';
import { ProfilePageComponent } from './pages/profile/profile';
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
      }
    ]
  }
];
