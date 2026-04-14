package com.team8.backend.domain.bid.service;

import com.team8.backend.domain.auction.entity.Auction;
import com.team8.backend.domain.auction.repository.AuctionRepository;
import com.team8.backend.domain.bid.dto.BidRequest;
import com.team8.backend.domain.bid.dto.BidResponse;
import com.team8.backend.domain.bid.entity.Bid;
import com.team8.backend.domain.bid.repository.BidRepository;
import com.team8.backend.domain.user.entity.User;
import com.team8.backend.domain.user.repository.UserRepository;
import com.team8.backend.global.exception.CustomException;
import com.team8.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BidService {

    private final BidRepository bidRepository;
    private final AuctionRepository auctionRepository;
    private final UserRepository userRepository;

    @Transactional
    public BidResponse createBid(BidRequest request) {
        Auction auction = auctionRepository.findById(request.auctionId())
                .orElseThrow(() -> new CustomException(ErrorCode.AUCTION_NOT_FOUND));

        User bidder = userRepository.findById(request.bidderId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        //1. 검증
        validateBid(auction, request,bidder);

        //2. 경매 상태변경
        auction.placeBid(bidder, request.bidPrice());
        try {

            //3.5분 자동연장 로직
            extendAuctionIfNeeded(auction);

            //4. 입찰 로그 저장
            Bid bid = new Bid(bidder, auction, request.bidPrice(), LocalDateTime.now());

            bidRepository.save(bid);

        } catch (CustomException e) {
            throw e; // 이미 정의된 에러는 그대로
        } catch (Exception e) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }


        return new BidResponse(
                auction.getAuctionId(),
                auction.getCurrentPrice(),
                auction.getLastBidder().getUserId()
        );
    }

    private void extendAuctionIfNeeded(Auction auction) {

        LocalDateTime now = LocalDateTime.now();

        if (auction.getEndedAt().minusMinutes(5).isBefore(now)) {
            auction.extendEndTime(5);
        }
    }

    private void validateBid(Auction auction, BidRequest request,User bidder) {
        // 1. 경매 종료 여부
        if (LocalDateTime.now().isAfter(auction.getEndedAt())) {
            throw new CustomException(ErrorCode.INVALID_AUCTION_STATE);
        }

        // 2. 현재가보다 높은지
        if (request.bidPrice() <= auction.getCurrentPrice()) {
            throw new CustomException(ErrorCode.BID_TOO_LOW);
        }

        // 3. 최소 입찰 단위 충족
        if ((request.bidPrice() - auction.getCurrentPrice()) < auction.getBidUnit()) {
            throw new CustomException(ErrorCode.INVALID_BID_UNIT);}

        // 4. 본인 경매 입찰 금지 (추가)
        if (auction.getSeller().getUserId().equals((bidder.getUserId()))) {
            throw new CustomException(ErrorCode.SELF_BIDDING);
        }

    }
}