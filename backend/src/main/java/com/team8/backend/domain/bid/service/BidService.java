package com.team8.backend.domain.bid.service;

import com.team8.backend.domain.auction.entity.Auction;
import com.team8.backend.domain.auction.entity.AuctionStatus;
import com.team8.backend.domain.auction.repository.AuctionRepository;
import com.team8.backend.domain.bid.dto.BidRequest;
import com.team8.backend.domain.bid.dto.BidResponse;
import com.team8.backend.domain.bid.dto.MyBidResponse;
import com.team8.backend.domain.bid.entity.Bid;
import com.team8.backend.domain.bid.event.BidEvent;
import com.team8.backend.domain.bid.repository.BidRepository;
import com.team8.backend.domain.user.entity.User;
import com.team8.backend.domain.user.repository.UserRepository;
import com.team8.backend.global.exception.CustomException;
import com.team8.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BidService {

    private final BidRepository bidRepository;
    private final AuctionRepository auctionRepository;
    private final UserRepository userRepository;

    private final ApplicationEventPublisher eventPublisher; //event->listender 전용 퍼블리셔
    private static final Logger log = LoggerFactory.getLogger(BidService.class);

    public List<MyBidResponse> getMyBids(Long userId) {
        return bidRepository.findLatestByBidderUserId(userId).stream()
                .map(MyBidResponse::from)
                .toList();
    }

    public List<MyBidResponse> getMyBidHistory(Long userId, Long auctionId) {
        return bidRepository.findAllByBidderUserIdAndAuctionIdOrderByBidedAtDesc(userId, auctionId).stream()
                .map(MyBidResponse::from)
                .toList();
    }

    @Transactional
    public BidResponse createBid(Long userId,BidRequest request) {
        Auction auction = auctionRepository.findByAuctionIdAndDeletedAtIsNull(request.auctionId())
                .orElseThrow(() -> new CustomException(ErrorCode.AUCTION_NOT_FOUND));

        //생성때마다 일일이 User를 쿼리하면 비효율적인데, 이왕 탈퇴나 id 변경기능 없으니 lazy 적용
        User bidder = userRepository.getReferenceById(userId);

        Bid bid;

        // TODO: validateBid에서 상태는 체크하지 않고 있음.
        //      추가로 현재 CustomException으로 catch 중인데 CustomException throw 되는 부분이 없음.
        //      save 시 낙관적 락으로 인해 에러가 날 수 있는데 Exception catch에 걸리고 INTERNAL_SERVER ERROR가 나오게 됨.
        //1. 검증
        validateBid(auction, request,bidder);

        //2. 경매 상태변경
        auction.placeBid(bidder, request.bidPrice());
        try {

            //3.5분 자동연장 로직
            extendAuctionIfNeeded(auction);
            //나중에 페이지 리로드 시켜야 될거 같음

            //4. 입찰 로그 저장
            bid = new Bid(bidder, auction, request.bidPrice(),LocalDateTime.now());

            bidRepository.save(bid);

        } catch (CustomException e) {
            throw e; // 이미 정의된 에러는 그대로
        } catch (Exception e) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        log.info("[BidService] 이벤트발행시작");
        // 🔥 이벤트 발행
        eventPublisher.publishEvent(

                new BidEvent(
                        bid.getAuction().getAuctionId(),
                        bid.getBidPrice(),
                        bid.getBidder().getUserId(),
                        bid.getBidedAt().toString(),
                        bidder.getNickname()
                )
        );

        log.info("[BidService] 이벤트발행종료");


        return new BidResponse(
                auction.getAuctionId(),
                auction.getCurrentPrice(),
                auction.getLastBidder().getUserId(),
                auction.getLastBidder().getCreatedAt(),
                auction.getLastBidder().getNickname()
        );
    }

    public List<BidResponse> getAuctionBids(Long auctionId) {
        return bidRepository.findByAuction_AuctionIdOrderByBidedAtDesc(auctionId)
                .stream()
                .map(bid -> new BidResponse(
                        bid.getAuction().getAuctionId(),
                        bid.getBidPrice(),
                        bid.getBidder().getUserId(),
                        bid.getBidedAt(),
                        bid.getBidder().getNickname()

                ))
                .toList();
    }
    private void extendAuctionIfNeeded(Auction auction) {

        LocalDateTime now = LocalDateTime.now();

        if (auction.getEndedAt().minusMinutes(5).isBefore(now)) {
            auction.extendEndTime(5);
        }
    }

    private void validateBid(Auction auction, BidRequest request,User bidder) {
        // 1. 경매 종료 여부
        if (LocalDateTime.now().isAfter(auction.getEndedAt()) || auction.getStatus() != AuctionStatus.ONGOING) {
            throw new CustomException(ErrorCode.INVALID_AUCTION_STATE);
        }

        // 2. 본인 경매 입찰 금지 (추가)
        if (auction.getSeller().getUserId().equals((bidder.getUserId()))) {
            throw new CustomException(ErrorCode.SELF_BIDDING);
        }

        // 3. 현재가보다 높은지
        if (request.bidPrice() <= auction.getCurrentPrice()) {
            throw new CustomException(ErrorCode.BID_TOO_LOW);
        }

        // 4. 최소 입찰 단위 충족
        if ((request.bidPrice() - auction.getCurrentPrice()) < auction.getBidUnit()) {
            throw new CustomException(ErrorCode.INVALID_BID_UNIT);}
    }
}
