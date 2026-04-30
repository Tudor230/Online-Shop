export interface CreateCheckoutSessionRequest {
  shippingAddressId: string;
  billingAddressId: string;
}

export interface CreateCheckoutSessionResponse {
  checkoutUrl: string;
  orderId: string;
}

export type CheckoutOrderStatus =
  | 'PENDING'
  | 'PAID'
  | 'PROCESSING'
  | 'SHIPPED'
  | 'DELIVERED'
  | 'CANCELLED'
  | 'RETURNED';

export interface CheckoutStatusResponse {
  orderId: string;
  status: CheckoutOrderStatus;
}
