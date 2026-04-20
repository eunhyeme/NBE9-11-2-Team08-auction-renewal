import { z } from "zod";

export const auctionSchema = z.object({
  title: z.string().min(1, { message: "제목을 입력해 주세요." }),
  description: z.string().min(1, { message: "상세 설명을 입력해 주세요." }),
  startPrice: z.coerce
    .number({ error: "숫자를 입력해 주세요." })
    .min(0, { message: "시작가는 0원 이상이어야 합니다." }),
  bidUnit: z.coerce
    .number({ error: "숫자를 입력해 주세요." })
    .min(100, { message: "입찰 단위는 최소 100원 이상입니다." })
    .refine((val) => val % 100 === 0, { 
      message: "입찰 단위는 100원 단위로 입력해 주세요." 
    }),
  duration: z.coerce
    .number({ error: "숫자를 입력해 주세요." })
    .min(1, { message: "최소 1일 이상 설정해야 합니다." })
    .max(10, { message: "최대 10일까지 설정 가능합니다." }),
  // 💡 기존 z.any()에서 z.instanceof(File)로 변경
  image: z.instanceof(File, { message: "이미지 파일을 업로드해 주세요." }),
});

export type AuctionFormValues = z.infer<typeof auctionSchema>;