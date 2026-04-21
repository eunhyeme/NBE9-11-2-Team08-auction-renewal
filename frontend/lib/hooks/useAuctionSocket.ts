// hooks/useAuctionSocket.ts

import {BidResponse} from "@/types/bid";
import {useEffect} from "react";
import {Client} from "@stomp/stompjs";

interface UseAuctionSocketProps {
    auctionId: number;
    onMessage: (bid: BidResponse) => void;
}

export function useAuctionSocket({
                                     auctionId,
                                     onMessage,
                                 }: UseAuctionSocketProps) {
    const wsBaseUrl = process.env.NEXT_PUBLIC_WS_URL || "";

    useEffect(() => {
        const client = new Client({
            brokerURL: `${wsBaseUrl}`,
            reconnectDelay: 5000,
        });

        client.onConnect = () => {
            client.subscribe(`/topic/auction/${auctionId}`, (message) => {
                const data: BidResponse = JSON.parse(message.body);
                onMessage(data);
            });
        };

        client.activate();

        return () => {
            void client.deactivate();
        }
    }, [auctionId, onMessage]);
}