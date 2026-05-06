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
  shippingAmount: number;
  taxAmount: number;
  discountAmount: number;
  totalAmount: number;
  currencyCode: string;
  items: OrderHistoryItem[];
}

export const canPayOrder = (order: OrderHistoryEntry): boolean => order.status === 'PENDING';

export const canCancelOrder = (order: OrderHistoryEntry): boolean => order.status === 'PENDING';

