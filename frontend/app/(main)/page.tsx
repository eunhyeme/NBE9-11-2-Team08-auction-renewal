import { AuctionGrid } from "@/components/auction/AuctionGrid";

export default function HomePage() {
  return (
    <div className="flex flex-col gap-6">
      <h1 className="text-2xl font-bold">진행 중인 경매</h1>
      <AuctionGrid />
    </div>
  );
}
