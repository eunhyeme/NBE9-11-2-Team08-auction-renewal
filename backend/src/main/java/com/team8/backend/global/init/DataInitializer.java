package com.team8.backend.global.init;

import com.team8.backend.domain.auction.entity.Auction;
import com.team8.backend.domain.auction.entity.AuctionStatus;
import com.team8.backend.domain.auction.repository.AuctionRepository;
import com.team8.backend.domain.bid.entity.Bid;
import com.team8.backend.domain.bid.repository.BidRepository;
import com.team8.backend.domain.order.entity.Order;
import com.team8.backend.domain.order.repository.OrderRepository;
import com.team8.backend.domain.user.entity.User;
import com.team8.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@Profile("dev")
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final OrderRepository orderRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) return;

        String encodedPassword = passwordEncoder.encode("1234");

        // ── USERS ──────────────────────────────────────────────
        User seller1 = User.create("seller1@test.com", encodedPassword, "판매왕김씨", "서울 강남구");
        seller1.grantBluecheck();
        seller1 = userRepository.save(seller1);

        User seller2 = User.create("seller2@test.com", encodedPassword, "중고천재", "부산 해운대구");
        seller2 = userRepository.save(seller2);

        User buyer1 = User.create("buyer1@test.com", encodedPassword, "입찰러A", "대전 유성구");
        buyer1 = userRepository.save(buyer1);

        User buyer2 = User.create("buyer2@test.com", encodedPassword, "입찰러B", "대구 수성구");
        buyer2 = userRepository.save(buyer2);

        User admin = User.createAdmin("admin@test.com", encodedPassword, "관리자", "서울");
        userRepository.save(admin);

        // ── AUCTIONS ───────────────────────────────────────────
        // 1. 진행중
        Auction auction1 = Auction.builder()
                .seller(seller1)
                .title("아이폰 14")
                .imageUrl("img1.jpg")
                .description("거의 새상품")
                .startPrice(500000L)
                .currentPrice(650000L)
                .lastBidder(buyer1)
                .bidUnit(10000)
                .status(AuctionStatus.ONGOING)
                .startedAt(LocalDateTime.of(2026, 4, 10, 10, 0))
                .endedAt(LocalDateTime.of(2026, 4, 20, 10, 0))
                .version(1L)
                .deletedAt(null)
                .build();
        auction1 = auctionRepository.save(auction1);

        // 2. 낙찰 완료
        Auction auction2 = Auction.builder()
                .seller(seller1)
                .title("맥북 프로")
                .imageUrl("img2.jpg")
                .description("M1 16GB")
                .startPrice(1000000L)
                .currentPrice(1300000L)
                .lastBidder(buyer2)
                .bidUnit(50000)
                .status(AuctionStatus.WON)
                .startedAt(LocalDateTime.of(2026, 3, 1, 10, 0))
                .endedAt(LocalDateTime.of(2026, 3, 5, 10, 0))
                .version(2L)
                .deletedAt(null)
                .build();
        auction2 = auctionRepository.save(auction2);

        // 3. 유찰
        Auction auction3 = Auction.builder()
                .seller(seller2)
                .title("갤럭시 탭")
                .imageUrl("img3.jpg")
                .description("사용감 있음")
                .startPrice(300000L)
                .currentPrice(300000L)
                .lastBidder(null)
                .bidUnit(10000)
                .status(AuctionStatus.UNSOLD)
                .startedAt(LocalDateTime.of(2026, 3, 1, 10, 0))
                .endedAt(LocalDateTime.of(2026, 3, 3, 10, 0))
                .version(1L)
                .deletedAt(null)
                .build();
        auctionRepository.save(auction3);

        // 4. 취소 (bid, order에서 미사용)
        Auction auction4 = Auction.builder()
                .seller(seller2)
                .title("에어팟")
                .imageUrl("img4.jpg")
                .description("미개봉")
                .startPrice(150000L)
                .currentPrice(150000L)
                .lastBidder(null)
                .bidUnit(5000)
                .status(AuctionStatus.CANCELED)
                .startedAt(LocalDateTime.of(2026, 4, 1, 10, 0))
                .endedAt(LocalDateTime.of(2026, 4, 2, 10, 0))
                .version(1L)
                .deletedAt(null)
                .build();
        auctionRepository.save(auction4);

        // ── BIDS ───────────────────────────────────────────────
        bidRepository.save(new Bid(buyer1, auction1, 510000L,  LocalDateTime.of(2026, 4, 10, 11, 0)));
        bidRepository.save(new Bid(buyer2, auction1, 600000L,  LocalDateTime.of(2026, 4, 10, 12, 0)));
        bidRepository.save(new Bid(buyer1, auction1, 650000L,  LocalDateTime.of(2026, 4, 10, 13, 0)));

        bidRepository.save(new Bid(buyer1, auction2, 1100000L, LocalDateTime.of(2026, 3, 1, 11, 0)));
        bidRepository.save(new Bid(buyer2, auction2, 1200000L, LocalDateTime.of(2026, 3, 2, 12, 0)));
        bidRepository.save(new Bid(buyer2, auction2, 1300000L, LocalDateTime.of(2026, 3, 3, 13, 0)));

        // ── ORDERS ─────────────────────────────────────────────
        // 1. 정상 결제 + 배송 완료
        Order order1 = new Order(
                auction2, buyer2, seller1,
                1300000L,
                LocalDateTime.of(2026, 3, 6, 10, 0),
                LocalDateTime.of(2026, 3, 5, 10, 0)
        );
        order1.pay(LocalDateTime.of(2026, 3, 5, 11, 0));
        order1.updateShippingAddress("대구 수성구");
        order1.startDelivery("TRACK1234");
        order1.completeDelivery();
        orderRepository.save(order1);

        // 2. 결제 대기 중
        Order order2 = new Order(
                auction1, buyer1, seller1,
                650000L,
                LocalDateTime.of(2026, 4, 21, 10, 0),
                LocalDateTime.of(2026, 4, 20, 10, 0)
        );
        orderRepository.save(order2);
    }
}
