package com.team8.backend.domain.bid.service;

import com.team8.backend.domain.auction.entity.Auction;
import com.team8.backend.domain.auction.entity.AuctionStatus;
import com.team8.backend.domain.auction.repository.AuctionRepository;
import com.team8.backend.domain.bid.dto.BidRequest;
import com.team8.backend.domain.bid.dto.BidResponse;
import com.team8.backend.domain.bid.dto.MyBidResponse;
import com.team8.backend.domain.bid.entity.Bid;
import com.team8.backend.domain.bid.repository.BidRepository;
import com.team8.backend.domain.user.entity.User;
import com.team8.backend.domain.user.repository.UserRepository;
import com.team8.backend.global.exception.CustomException;
import com.team8.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@SpringBootTest
class BidServiceTest {

    // 실제 스프링 빈으로 주입
    @Autowired
    private BidService bidService;

    // 외부 의존성은 모두 MockitoBean으로 교체
    @MockitoBean private BidRepository bidRepository;
    @MockitoBean private AuctionRepository auctionRepository;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private ApplicationEventPublisher eventPublisher;

    private static final Long USER_ID   = 2L;
    private static final Long AUCTION_ID = 10L;

    // 항상 현재 기준 미래로 설정 (과거 고정값이면 INVALID_AUCTION_STATE 선발생)
    private static final LocalDateTime FIXED_TIME =
            LocalDateTime.now().plusDays(1);

    private User seller;
    private User bidder;
    private Auction auction;

    @BeforeEach
    void setUp() {
        seller  = mockUser(1L, "seller");
        bidder  = mockUser(USER_ID, "bidder");
        auction = mockAuction(AUCTION_ID, seller, 1000L, 500L, FIXED_TIME.plusMinutes(30));
    }

    // ═══════════════════════════════════════════════
    // createBid
    // ═══════════════════════════════════════════════
    @Nested
    @DisplayName("입찰생성")
    class CreateBid {

        @Test
        @DisplayName("정상 입찰 → 저장 + 이벤트 발행")
        void success() {
            BidRequest req = new BidRequest(AUCTION_ID, 2000L);

            given(auctionRepository.findByAuctionIdAndDeletedAtIsNull(AUCTION_ID)).willReturn(Optional.of(auction));
            given(userRepository.getReferenceById(USER_ID)).willReturn(bidder);
            given(bidRepository.save(any(Bid.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            BidResponse result = bidService.createBid(USER_ID, req);

            assertThat(result).isNotNull();

            // 저장 검증
            ArgumentCaptor<Bid> bidCaptor = ArgumentCaptor.forClass(Bid.class);
            verify(bidRepository).save(bidCaptor.capture());
            assertThat(bidCaptor.getValue().getBidPrice()).isEqualTo(2000L);

            // @SpringBootTest에서 ApplicationEventPublisher는 스프링 내부 빈이 사용되므로
            // @MockitoBean으로 교체되지 않습니다.
            // 이벤트 발행 검증은 BidEventListenerSpringTest에서 수행합니다.
            // 여기서는 서비스 로직(저장)만 검증합니다.
        }

        @Test
        @DisplayName("경매 없음 → 예외")
        void auctionNotFound() {
            given(auctionRepository.findByAuctionIdAndDeletedAtIsNull(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() ->
                    bidService.createBid(USER_ID, new BidRequest(999L, 2000L)))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(ErrorCode.AUCTION_NOT_FOUND);
        }

        @Test
        @DisplayName("취소된 경매 입찰 -> INVALID_AUCTION_STATE 예외") // 이름 수정
        void bidToWithDrawAuction() {
            given(auctionRepository.findByAuctionIdAndDeletedAtIsNull(AUCTION_ID)).willReturn(Optional.of(auction));
            given(auction.getStatus()).willReturn(AuctionStatus.CANCELED);

            // 이 줄은 제거해도 무방하지만 남겨두셔도 됩니다.
            // given(auction.getDeletedAt()).willReturn(LocalDateTime.now());

            assertThatThrownBy(() ->
                    bidService.createBid(USER_ID,
                            new BidRequest(auction.getAuctionId(), 11_000L))
            )
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    // 수정: AUCTION_NOT_FOUND -> INVALID_AUCTION_STATE
                    .isEqualTo(ErrorCode.INVALID_AUCTION_STATE);
        }

        @Test
        @DisplayName("유찰된 경매 입찰 -> INVALID_AUCTION_STATE 예외")
        void bidToUnsoldAuction() {
            given(auctionRepository.findByAuctionIdAndDeletedAtIsNull(AUCTION_ID))
                    .willReturn(Optional.of(auction));
            given(auction.getStatus()).willReturn(AuctionStatus.UNSOLD);
            given(userRepository.getReferenceById(USER_ID)).willReturn(bidder);

            assertThatThrownBy(() ->
                    bidService.createBid(USER_ID,
                            new BidRequest(auction.getAuctionId(), 11_000L))
            )
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_AUCTION_STATE);
        }

        @Test
        @DisplayName("낙찰된 경매 입찰 -> INVALID_AUCTION_STATE 예외")
        void bidToWonAuction() {
            given(auctionRepository.findByAuctionIdAndDeletedAtIsNull(AUCTION_ID))
                    .willReturn(Optional.of(auction));
            given(auction.getStatus()).willReturn(AuctionStatus.WON);
            given(userRepository.getReferenceById(USER_ID)).willReturn(bidder);

            assertThatThrownBy(() ->
                    bidService.createBid(USER_ID,
                            new BidRequest(auction.getAuctionId(), 11_000L))
            )
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_AUCTION_STATE);
        }

        @Test
        @DisplayName("자기 경매 입찰 → 예외")
        void selfBidding() {
            given(auctionRepository.findByAuctionIdAndDeletedAtIsNull(AUCTION_ID)).willReturn(Optional.of(auction));
            given(userRepository.getReferenceById(1L)).willReturn(seller);

            assertThatThrownBy(() ->
                    bidService.createBid(1L, new BidRequest(AUCTION_ID, 2000L)))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(ErrorCode.SELF_BIDDING);
        }

        @Test
        @DisplayName("종료 임박 → 시간 연장 호출")
        void extendTime() {
            Auction nearEnd = mockAuction(12L, seller, 1000L, 500L, LocalDateTime.now().plusMinutes(3));

            given(auctionRepository.findByAuctionIdAndDeletedAtIsNull(12L)).willReturn(Optional.of(nearEnd));
            given(userRepository.getReferenceById(USER_ID)).willReturn(bidder);
            given(bidRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            bidService.createBid(USER_ID, new BidRequest(12L, 2000L));

            verify(nearEnd).extendEndTime(5L);
        }
    }

    // ═══════════════════════════════════════════════
    // createBidAtomic
    // ═══════════════════════════════════════════════
    @Nested
    @DisplayName("입찰생성(Atomic) — 원자 UPDATE 방식")
    class CreateBidAtomic {

        @Test
        @DisplayName("정상 입찰 → 저장")
        void success() {
            given(auctionRepository.findByAuctionIdAndDeletedAtIsNull(AUCTION_ID))
                    .willReturn(Optional.of(auction));
            given(userRepository.getReferenceById(USER_ID)).willReturn(bidder);
            given(auctionRepository.atomicBidUpdate(eq(AUCTION_ID), eq(2000L), eq(USER_ID), any(), any())).willReturn(1);
            given(bidRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            BidResponse result = bidService.createBidAtomic(USER_ID, new BidRequest(AUCTION_ID, 2000L));

            assertThat(result).isNotNull();
            assertThat(result.bidPrice()).isEqualTo(2000L);
            verify(bidRepository).save(any(Bid.class));
        }

        @Test
        @DisplayName("원자 UPDATE 0건 → 재조회 후 BID_TOO_LOW")
        void atomicUpdateFails_bidTooLow() {
            given(auctionRepository.findByAuctionIdAndDeletedAtIsNull(AUCTION_ID))
                    .willReturn(Optional.of(auction));
            given(userRepository.getReferenceById(USER_ID)).willReturn(bidder);
            given(auctionRepository.atomicBidUpdate(eq(AUCTION_ID), eq(2000L), eq(USER_ID), any(), any())).willReturn(0);

            assertThatThrownBy(() ->
                    bidService.createBidAtomic(USER_ID, new BidRequest(AUCTION_ID, 2000L)))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(ErrorCode.BID_TOO_LOW);
        }

        @Test
        @DisplayName("원자 UPDATE 0건 + 경매 종료 → 재조회 후 INVALID_AUCTION_STATE")
        void atomicUpdateFails_auctionEnded() {
            Auction ended = mockAuction(AUCTION_ID, seller, 1000L, 500L,
                    LocalDateTime.now().minusMinutes(1));

            // 사전 읽기 없이 바로 atomicBidUpdate → 실패 시 재조회 1회만 호출
            given(auctionRepository.findByAuctionIdAndDeletedAtIsNull(AUCTION_ID))
                    .willReturn(Optional.of(ended));
            given(auctionRepository.atomicBidUpdate(eq(AUCTION_ID), eq(2000L), eq(USER_ID), any(), any())).willReturn(0);

            assertThatThrownBy(() ->
                    bidService.createBidAtomic(USER_ID, new BidRequest(AUCTION_ID, 2000L)))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_AUCTION_STATE);
        }

        @Test
        @DisplayName("종료된 경매 → 사전 검증에서 INVALID_AUCTION_STATE")
        void preCheck_auctionEnded() {
            given(auctionRepository.findByAuctionIdAndDeletedAtIsNull(AUCTION_ID))
                    .willReturn(Optional.of(auction));
            given(auction.getStatus()).willReturn(AuctionStatus.WON);
            given(userRepository.getReferenceById(USER_ID)).willReturn(bidder);

            assertThatThrownBy(() ->
                    bidService.createBidAtomic(USER_ID, new BidRequest(AUCTION_ID, 2000L)))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_AUCTION_STATE);
        }

        @Test
        @DisplayName("자기 경매 입찰 → 사전 검증에서 SELF_BIDDING")
        void preCheck_selfBidding() {
            given(auctionRepository.findByAuctionIdAndDeletedAtIsNull(AUCTION_ID))
                    .willReturn(Optional.of(auction));
            given(userRepository.getReferenceById(1L)).willReturn(seller);

            assertThatThrownBy(() ->
                    bidService.createBidAtomic(1L, new BidRequest(AUCTION_ID, 2000L)))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(ErrorCode.SELF_BIDDING);
        }
    }

    // ═══════════════════════════════════════════════
    // getAuctionBids
    // ═══════════════════════════════════════════════
    @Nested
    class GetAuctionBids {

        @Test
        void success() {
            Bid bid1 = new Bid(bidder, auction, 2000L, FIXED_TIME);
            Bid bid2 = new Bid(bidder, auction, 1500L, FIXED_TIME.minusMinutes(5));

            given(bidRepository.findByAuction_AuctionIdOrderByBidedAtDesc(AUCTION_ID))
                    .willReturn(List.of(bid1, bid2));

            List<BidResponse> result = bidService.getAuctionBids(AUCTION_ID);

            assertThat(result).hasSize(2);
            assertThat(result.getFirst().bidPrice()).isEqualTo(2000L);
            assertThat(result.get(1).bidPrice()).isEqualTo(1500L);
        }
    }

    // ═══════════════════════════════════════════════
    // getMyBids
    // ═══════════════════════════════════════════════
    @Nested
    class GetMyBids {

        @Test
        void success() {
            Bid bid = new Bid(bidder, auction, 2000L, FIXED_TIME);
            given(bidRepository.findLatestByBidderUserId(USER_ID)).willReturn(List.of(bid));

            List<MyBidResponse> result = bidService.getMyBids(USER_ID);

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().myBidPrice()).isEqualTo(2000L);
        }
    }

    // ═══════════════════════════════════════════════
    // 헬퍼
    // ═══════════════════════════════════════════════

    private User mockUser(Long userId, String nickname) {
        User user = mock(User.class);
        given(user.getUserId()).willReturn(userId);
        given(user.getNickname()).willReturn(nickname);
        given(user.getCreatedAt()).willReturn(FIXED_TIME);
        return user;
    }

    private Auction mockAuction(Long auctionId, User seller,
                                Long currentPrice, Long bidUnit,
                                LocalDateTime endedAt) {
        Auction auction = mock(Auction.class);

        given(auction.getAuctionId()).willReturn(auctionId);
        given(auction.getSeller()).willReturn(seller);
        given(auction.getCurrentPrice()).willReturn(currentPrice);
        given(auction.getBidUnit()).willReturn(Math.toIntExact(bidUnit));

        final LocalDateTime[] holder = {endedAt};
        given(auction.getEndedAt()).willAnswer(inv -> holder[0]);
        doAnswer(inv -> {
            holder[0] = holder[0].plusMinutes(inv.getArgument(0));
            return null;
        }).when(auction).extendEndTime(anyInt());

        given(auction.getLastBidder()).willReturn(seller);
        given(auction.getStatus()).willReturn(AuctionStatus.ONGOING);

        return auction;
    }
}
