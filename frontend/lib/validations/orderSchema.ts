import { z } from "zod";

export const shippingSchema = z.object({
  shippingAddress: z.string().min(1, { error: "배송지를 입력해 주세요." }),
});

export const trackingSchema = z.object({
  carrier: z.string().min(1, { error: "택배사를 선택해 주세요." }),
  trackingNumber: z
    .string()
    .min(1, { error: "송장번호를 입력해 주세요." })
    .regex(/^\d+$/, { error: "송장번호는 숫자만 입력 가능합니다." }),
});

export type ShippingFormValues = z.infer<typeof shippingSchema>;
export type TrackingFormValues = z.infer<typeof trackingSchema>;
