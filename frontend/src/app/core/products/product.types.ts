export interface ProductSummary {
  id: string;
  slug: string;
  category: string;
  title: string;
  rating: number;
  reviewCount: number;
  price: number;
  imageId: string;
}

export interface ProductDetails {
  id: string;
  slug: string;
  category: string;
  title: string;
  rating: number;
  reviewCount: number;
  canReview: boolean;
  hasReviewed: boolean;
  reviewedReviewId: string | null;
  price: number;
  description: string;
  detailedDescription: string;
  imageId: string;
  imageGalleryIds: string[];
  reviews: ProductReview[];
}

export interface ProductReview {
  id: string;
  rating: number;
  comment: string;
  reviewerName: string;
  createdAt: string;
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

export interface CreateProductReviewRequest {
  rating: number;
  comment: string;
}

export interface CategoryTreeNode {
  id: string;
  parentId: string | null;
  name: string;
  slug: string;
  path: string;
  children: CategoryTreeNode[];
}
