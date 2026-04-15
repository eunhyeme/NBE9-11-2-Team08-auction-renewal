import { z } from "zod";

export const auctionCreateSchema = z.object({
  title: z.string().min(2, { error: "제목은 2자 이상이어야 합니다." }),
  description: z.string().optional(),
  imageUrl: z.string().url({ error: "유효한 이미지 URL을 입력해 주세요." }).optional().or(z.literal("")),
  startPrice: z.coerce
    .number()
    .min(1000, { error: "시작가는 1,000원 이상이어야 합니다." }),
  bidUnit: z.coerce
    .number()
    .min(100, { error: "입찰 단위는 100원 이상이어야 합니다." }),
  startedAt: z.string().min(1, { error: "시작 시간을 선택해 주세요." }),
  endedAt: z.string().min(1, { error: "종료 시간을 선택해 주세요." }),
});

export type AuctionCreateFormValues = z.infer<typeof auctionCreateSchema>;
