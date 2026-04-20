import { z } from "zod";

export const shippingSchema = z.object({
  shippingAddress: z.string().min(1, { message: "배송지를 입력해 주세요." }),
  saveAsDefault: z.boolean(), // 기본 배송지 저장 
});

export const trackingSchema = z.object({
  carrier: z.string().min(1, { message: "택배사를 선택해 주세요." }),
  trackingNumber: z
    .string()
    .min(1, { message: "송장번호를 입력해 주세요." })
    .regex(/^\d+$/, { message: "송장번호는 숫자만 입력 가능합니다." }),
});

export type ShippingFormValues = z.infer<typeof shippingSchema>;
export type TrackingFormValues = z.infer<typeof trackingSchema>;