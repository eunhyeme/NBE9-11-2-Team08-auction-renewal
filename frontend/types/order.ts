export type OrderStatus =
  | "AWAITING_PAYMENT"
  | "PAID"
  | "DELIVERING"
  | "DELIVERED"
  | "NOT_PAID"
  | "NOT_DELIVERED";

export interface OrderResponse {
  orderId: number;
  auctionId: number;
  auctionTitle: string;
  auctionImageUrl: string | null;
  bidderId: number;
  sellerId: number;
  finalPrice: number;
  status: OrderStatus;
  paymentDeadline: string;
  paidAt: string | null;
  shippingAddress: string | null;
  trackingNumber: string | null;
  createdAt: string;
  deliveryDeadline: string | null;
}
