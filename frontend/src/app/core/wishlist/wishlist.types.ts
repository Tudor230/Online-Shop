export interface WishlistItem {
  productSlug: string;
  title: string;
  price: number;
  imageLabel: string;
  createdAt: string;
}

export interface WishlistState {
  items: WishlistItem[];
  totalItems: number;
}
