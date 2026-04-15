/**
 * 숫자를 한국 원화 포맷으로 변환한다.
 * 예: 150000 → "150,000원"
 */
export function formatPrice(amount: number): string {
  return `${amount.toLocaleString("ko-KR")}원`;
}
