import { Routes } from '@angular/router';
import { ProductDetailsComponent } from './pages/product-details/product-details';
import { ProductGridComponent } from './pages/product-grid/product-grid';

export const routes: Routes = [
  {
    path: '',
    component: ProductGridComponent
  },
  {
    path: 'product/:id',
    component: ProductDetailsComponent
  }
];
