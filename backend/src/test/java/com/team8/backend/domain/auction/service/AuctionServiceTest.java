package com.team8.backend.domain.auction.service;

import com.team8.backend.domain.auction.dto.response.AuctionResponse;
import com.team8.backend.domain.auction.entity.Auction;
import com.team8.backend.domain.auction.entity.AuctionStatus;
import com.team8.backend.domain.auction.repository.AuctionRepository;
import com.team8.backend.domain.order.service.OrderService;
import com.team8.backend.domain.user.entity.User;
import com.team8.backend.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuctionServiceTest {

    @Mock private AuctionRepository auctionRepository;
    @Mock private UserRepository userRepository;
    @Mock private FileService fileService;
    @Mock private OrderService orderService;

    @InjectMocks private AuctionService auctionService;

    private Auction stubAuction(String title, Long price) {
        User seller = User.create("s@test.com", "pw", "판매자", null);
        return Auction.builder()
                .seller(seller)
                .title(title)
                .imageUrl("/img.jpg")
                .description("설명")
                .startPrice(price)
                .currentPrice(price)
                .bidUnit(1000)
                .status(AuctionStatus.ONGOING)
                .startedAt(LocalDateTime.now().plusDays(1))
                .endedAt(LocalDateTime.now().plusDays(3))
                .build();
    }

    @Test
    @DisplayName("검색 파라미터 없으면 전체 목록 반환")
    void getAuctions_noParams_returnsAll() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Auction> page = new PageImpl<>(List.of(stubAuction("아이폰", 500000L)));
        given(auctionRepository.findAll(any(Specification.class), any(Pageable.class))).willReturn(page);

        Page<AuctionResponse> result = auctionService.getAuctions(pageable, null, null, null, false);

        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(auctionRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("검색 파라미터가 있어도 findAll(Specification, Pageable)을 호출")
    void getAuctions_withParams_callsFindAllWithSpec() {
        Pageable pageable = PageRequest.of(0, 10);
        given(auctionRepository.findAll(any(Specification.class), any(Pageable.class)))
                .willReturn(Page.empty());

        auctionService.getAuctions(pageable, "맥북", 100000L, 2000000L, true);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Specification<Auction>> specCaptor = ArgumentCaptor.forClass(Specification.class);
        verify(auctionRepository).findAll(specCaptor.capture(), any(Pageable.class));
        assertThat(specCaptor.getValue()).isNotNull();
    }

    @Test
    @DisplayName("결과를 AuctionResponse로 변환하여 반환")
    void getAuctions_mapsToDto() {
        Pageable pageable = PageRequest.of(0, 10);
        Auction auction = stubAuction("갤럭시", 300000L);
        given(auctionRepository.findAll(any(Specification.class), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(auction)));

        Page<AuctionResponse> result = auctionService.getAuctions(pageable, null, null, null, false);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).title()).isEqualTo("갤럭시");
        assertThat(result.getContent().get(0).currentPrice()).isEqualTo(300000L);
    }
}
