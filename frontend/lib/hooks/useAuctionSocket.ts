// hooks/useAuctionSocket.ts

import { useEffect, useRef } from "react";
import { Client } from "@stomp/stompjs";
import { BidResponse } from "@/types/bid";

interface UseAuctionSocketProps {
    auctionId: number;
    enabled?: boolean;
    onMessage: (bid: BidResponse) => void;
    onEnd?: (data: any) => void; // END 이벤트 대응 (선택)
}

interface AuctionEvent<T> {
    type: string;
    data: T;
}

export function useAuctionSocket({
                                     auctionId,
                                     enabled = true,
                                     onMessage,
                                     onEnd,
                                 }: UseAuctionSocketProps) {
    const wsBaseUrl = process.env.NEXT_PUBLIC_WS_URL || "";
    const clientRef = useRef<Client | null>(null);

    useEffect(() => {
        if (!enabled) return;

        const client = new Client({
            brokerURL: wsBaseUrl,
            reconnectDelay: 5000,
            debug: (str) => {
                console.log("[STOMP]", str);
            },
        });

        clientRef.current = client;

        client.onConnect = () => {
            client.subscribe(`/topic/auction/${auctionId}`, (message) => {
                try {
                    const event: AuctionEvent<BidResponse> = JSON.parse(message.body);

                    switch (event.type) {
                        case "BID":
                            // 입찰 갱신
                            onMessage(event.data);
                            break;

                        case "END":
                            // 경매 종료 처리
                            console.log("Auction ended");

                            if (onEnd) {
                                onEnd(event.data);
                            }

                            // WebSocket 종료
                            clientRef.current?.deactivate();
                            clientRef.current = null;
                            break;

                        default:
                            console.warn("Unknown event type:", event.type);
                            break;
                    }
                } catch (err) {
                    console.error("WebSocket message parse error:", err);
                }
            });
        };

        client.onStompError = (frame) => {
            console.error("STOMP error:", frame.headers["message"]);
            console.error("Details:", frame.body);
        };

        client.activate();

        return () => {
            clientRef.current?.deactivate();
            clientRef.current = null;
        };
    }, [auctionId, enabled, onMessage, onEnd, wsBaseUrl]);
}