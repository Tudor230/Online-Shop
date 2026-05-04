export interface WishlistItem {
  productId: string;
  title: string;
  price: number;
  imageLabel: string;
  addedAt: string;
}

export interface WishlistState {
  items: WishlistItem[];
  totalItems: number;
}
