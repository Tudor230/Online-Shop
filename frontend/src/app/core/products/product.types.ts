export interface ProductSummary {
  id: string;
  category: string;
  title: string;
  rating: number;
  reviewCount: number;
  price: number;
  imagePlaceholder: string;
}

export interface ProductDetails {
  id: string;
  category: string;
  title: string;
  rating: number;
  reviewCount: number;
  price: number;
  description: string;
  imagePlaceholder: string;
  imageGallery: string[];
}

