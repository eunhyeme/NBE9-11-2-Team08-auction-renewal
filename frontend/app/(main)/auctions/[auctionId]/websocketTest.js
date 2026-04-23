import { Client } from "@stomp/stompjs";

//웹소켓 연결 테스트용 코드
const client = new Client({
    brokerURL: "ws://localhost:8080/ws",
});

client.onConnect = () => {
    console.log("연결 성공");

    client.subscribe("/topic/auction/5", msg => {
        console.log("받은 데이터:", msg.body);
    });

    client.publish({
        destination: "/app/bid",
        body: JSON.stringify({ auctionId: 3, price: 1000 }),
    });
};

client.activate();
