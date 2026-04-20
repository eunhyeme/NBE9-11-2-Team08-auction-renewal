import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  getOrder,
  getMyOrders,
  payOrder,
  updateShipping,
  registerTracking,
  type OrderHistoryFilter,
  type OrderHistoryRole,
} from "@/lib/api/order";
import { MOCK_ORDERS } from "@/lib/mocks/data";


const USE_MOCK = process.env.NEXT_PUBLIC_USE_MOCK === "true";

export function useOrder(orderId: number) {
  return useQuery({
    queryKey: ["order", orderId],
    queryFn: USE_MOCK
      ? () =>
          Promise.resolve(
            MOCK_ORDERS.find((o) => o.orderId === orderId) ?? MOCK_ORDERS[0],
          )
      : () => getOrder(orderId),
    enabled: !!orderId,
  });
}

export function useMyOrders(
  role: OrderHistoryRole,
  filter: OrderHistoryFilter,
  enabled = true,
) {
  return useQuery({
    queryKey: ["myOrders", role, filter],
    queryFn: USE_MOCK
      ? () => Promise.resolve(MOCK_ORDERS)
      : () => getMyOrders(role, filter),
    enabled,
  });
}

export function usePayOrder(orderId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: USE_MOCK
      ? () => Promise.resolve()
      : () => payOrder(orderId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["order", orderId] });
    },
  });
}

export function useUpdateShipping(orderId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ address, saveAsDefault }: { address: string; saveAsDefault: boolean }) => 
      updateShipping(orderId, address, saveAsDefault),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["order", orderId] });
      queryClient.invalidateQueries({ queryKey: ["me"] });
    },
  });
}

export function useRegisterTracking(orderId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: USE_MOCK
      ? (_: string) => Promise.resolve()
      : (trackingNumber: string) => registerTracking(orderId, trackingNumber),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["order", orderId] });
    },
  });
}
