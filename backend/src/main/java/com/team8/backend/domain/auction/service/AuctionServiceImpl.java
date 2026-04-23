package com.team8.backend.domain.auction.service;

import com.team8.backend.domain.auction.dto.response.AuctionResponse;
import com.team8.backend.domain.auction.entity.Auction;
import com.team8.backend.domain.auction.entity.AuctionStatus;
import com.team8.backend.domain.auction.event.AuctionEvent;
import com.team8.backend.domain.auction.repository.AuctionRepository;
import com.team8.backend.domain.auction.repository.AuctionSpecification;
import com.team8.backend.domain.order.service.OrderService;
import com.team8.backend.global.exception.CustomException;
import com.team8.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuctionServiceImpl implements AuctionService {

    private final AuctionRepository auctionRepository;
    private final OrderService orderService;
    private final ApplicationEventPublisher eventPublisher; //event->listender 전용 퍼블리셔

    @Override
    public Page<AuctionResponse> getAuctions(
            Pageable pageable, String keyword, Long minPrice, Long maxPrice, List<AuctionStatus> statuses) {
        Specification<Auction> spec = AuctionSpecification.notDeleted();

        List<AuctionStatus> allowedStatuses = (statuses == null || statuses.isEmpty())
                ? List.of(AuctionStatus.ONGOING)
                : statuses.stream().filter(s -> s != AuctionStatus.CANCELED).toList();

        if (allowedStatuses.isEmpty()) {
            allowedStatuses = List.of(AuctionStatus.ONGOING);
        }

        spec = spec.and(AuctionSpecification.statusIn(allowedStatuses));

        if (keyword != null && !keyword.isBlank()) {
            spec = spec.and(AuctionSpecification.titleContains(keyword));
        }
        if (minPrice != null) {
            spec = spec.and(AuctionSpecification.minPrice(minPrice));
        }
        if (maxPrice != null) {
            spec = spec.and(AuctionSpecification.maxPrice(maxPrice));
        }
        return auctionRepository.findAll(spec, pageable).map(AuctionResponse::from);
    }

    @Override
    public AuctionResponse getAuction(Long auctionId) {
        Auction auction = auctionRepository.findByAuctionIdAndDeletedAtIsNull(auctionId)
                .orElseThrow(() -> new CustomException(ErrorCode.AUCTION_NOT_FOUND));
        return AuctionResponse.from(auction);
    }

    @Override
    public List<AuctionResponse> getMyAuctions(Long userId) {
        return auctionRepository.findAllBySeller_UserIdAndDeletedAtIsNullOrderByStartedAtDesc(userId).stream()
                .map(AuctionResponse::from)
                .toList();
    }

    @Override
    public AuctionResponse getMyAuction(Long userId, Long auctionId) {
        Auction auction = auctionRepository.findByAuctionIdAndSeller_UserIdAndDeletedAtIsNull(auctionId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.AUCTION_NOT_FOUND));
        return AuctionResponse.from(auction);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void closeAuction(Long auctionId) {
        Auction auction = auctionRepository.findByAuctionIdAndDeletedAtIsNull(auctionId)
                .orElseThrow(() -> new CustomException(ErrorCode.AUCTION_NOT_FOUND));

        if (auction.getStatus() != AuctionStatus.ONGOING) {
            return;
        }

        auction.close();

        if (auction.getStatus() == AuctionStatus.WON) {
            orderService.createOrder(auction, auction.getLastBidder(), auction.getCurrentPrice());
        }

        // 성공시 웹소켓 연결 끊는 이벤트 발행 (트랜잭션 성공 시에만)
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                eventPublisher.publishEvent(new AuctionEvent(auction.getAuctionId()));
            }
        });
    }
}
