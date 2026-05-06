export interface ProductSummary {
  id: string;
  category: string;
  title: string;
  rating: number;
  reviewCount: number;
  price: number;
  imageId: string;
}

export interface ProductDetails {
  id: string;
  category: string;
  title: string;
  rating: number;
  reviewCount: number;
  price: number;
  description: string;
  imageId: string;
  imageGalleryIds: string[];
}

export interface ProductSearchPage {
  items: ProductSummary[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
  hasPrevious: boolean;
  hasNext: boolean;
}

