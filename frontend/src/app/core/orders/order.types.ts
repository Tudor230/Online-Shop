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
  category: string;
  title: string;
  imageId: string;
  description: string;
  productRating: number;
  productReviewCount: number;
  quantity: number;
  unitPrice: number;
  lineTotal: number;
  canLeaveReview: boolean;
  reviewed: boolean;
  reviewId: string | null;
  reviewedRating: number | null;
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

export type OrderDetailsEntry = OrderHistoryEntry;

export const canPayOrder = (order: OrderHistoryEntry): boolean => order.status === 'PENDING';

export const canCancelOrder = (order: OrderHistoryEntry): boolean => order.status === 'PENDING';

