export interface CartItem {
  productId: string;
  title: string;
  price: number;
  imageLabel: string;
  quantity: number;
}

export interface CartState {
  items: CartItem[];
  totalItems: number;
}
