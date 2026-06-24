export interface WishlistItem {
  productId: string;
  productSlug: string;
  title: string;
  price: number;
  imageId: string;
  createdAt: string;
}

export interface WishlistState {
  items: WishlistItem[];
  totalItems: number;
}
