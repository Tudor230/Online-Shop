import { RenderMode, ServerRoute } from '@angular/ssr';
import { mockProducts } from '../assets/data/mock-products';

export const serverRoutes: ServerRoute[] = [
  {
    path: 'product/:id',
    renderMode: RenderMode.Prerender,
    async getPrerenderParams() {
      return mockProducts.map((product) => ({ id: product.id }));
    }
  },
  {
    path: 'products',
    renderMode: RenderMode.Prerender
  },
  {
    path: '**',
    renderMode: RenderMode.Prerender
  }
];
