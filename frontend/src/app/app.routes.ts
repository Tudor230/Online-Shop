import { Routes } from '@angular/router';
import { ProductDetailsComponent } from './pages/product-details/product-details';
import { ProductGridComponent } from './pages/product-grid/product-grid';
import { WelcomeComponent } from './pages/welcome/welcome';

export const routes: Routes = [
  {
    path: '',
    component: WelcomeComponent
  },
  {
    path: 'products',
    component: ProductGridComponent
  },
  {
    path: 'product/:id',
    component: ProductDetailsComponent
  },
  {
    path: 'welcome',
    redirectTo: '',
    pathMatch: 'full'
  }
];
