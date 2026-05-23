export interface CartItem {
  productId: string;
  title: string;
  price: number;
  imageId: string;
  quantity: number;
}

export interface CartState {
  items: CartItem[];
  totalItems: number;
  subtotal: number;
  shippingAmount: number;
  taxAmount: number;
  totalAmount: number;
  currencyCode: string;
}
