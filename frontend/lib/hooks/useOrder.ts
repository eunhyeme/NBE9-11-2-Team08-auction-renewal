import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { getOrder, payOrder, updateShipping, registerTracking } from "@/lib/api/order";
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
    mutationFn: USE_MOCK
      ? (_: string) => Promise.resolve()
      : (shippingAddress: string) => updateShipping(orderId, shippingAddress),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["order", orderId] });
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
