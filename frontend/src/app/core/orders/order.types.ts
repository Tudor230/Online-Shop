export type OrderStatus =
  | 'PENDING'
  | 'PAID'
  | 'PROCESSING'
  | 'SHIPPED'
  | 'DELIVERED'
  | 'CANCELLED'
  | 'RETURNED';

export interface OrderHistoryItem {
  productSlug: string;
  title: string;
  imageId: string;
  quantity: number;
  unitPrice: number;
  lineTotal: number;
}

export interface OrderHistoryEntry {
  id: string;
  orderNumber: string;
  status: OrderStatus;
  createdAt: string;
  subtotal: number;
  discountAmount: number;
  totalAmount: number;
  items: OrderHistoryItem[];
}
