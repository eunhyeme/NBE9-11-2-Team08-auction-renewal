import { apiFetch } from "./client";
import type { ApiResponse } from "@/types/api";
import type { OrderResponse } from "@/types/order";

export async function getOrder(orderId: number): Promise<OrderResponse> {
  const res = await apiFetch.get<ApiResponse<OrderResponse>>(
    `/api/orders/${orderId}`,
  );
  return res.result;
}

export async function payOrder(orderId: number): Promise<void> {
  await apiFetch.post(`/api/orders/${orderId}/pay`);
}

export async function updateShipping(
  orderId: number,
  shippingAddress: string,
  saveAsDefault: boolean,
): Promise<void> {
  await apiFetch.post(`/api/orders/${orderId}/shipping`, { 
    shippingAddress, 
    saveAsDefault 
  });
}

export async function registerTracking(
  orderId: number,
  trackingNumber: string,
): Promise<void> {
  await apiFetch.post(`/api/orders/${orderId}/tracking`, { trackingNumber });
}