// hooks/useAuctionSocket.ts

import {BidResponse} from "@/types/bid";
import {useEffect} from "react";
import {Client} from "@stomp/stompjs";

interface UseAuctionSocketProps {
    auctionId: number;
    enabled?:boolean;
    onMessage: (bid: BidResponse) => void;

}

export function useAuctionSocket({
                                     auctionId,
                                     enabled=true,
                                     onMessage,
                                 }: UseAuctionSocketProps) {
    const wsBaseUrl = process.env.NEXT_PUBLIC_WS_URL || "";

    useEffect(() => {
        // ongoing이 아닐때 연결 안함
        if (!enabled) return;

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
    }, [auctionId, enabled, onMessage, wsBaseUrl]);}