package com.team8.backend.domain.order.facade;

import com.team8.backend.domain.auction.entity.Auction;
import com.team8.backend.domain.auction.entity.AuctionStatus;
import com.team8.backend.domain.auction.repository.AuctionRepository;
import com.team8.backend.domain.order.dto.request.OrderAddressRequest;
import com.team8.backend.domain.order.entity.Order;
import com.team8.backend.domain.order.repository.OrderRepository;
import com.team8.backend.domain.user.entity.User;
import com.team8.backend.domain.user.repository.UserRepository;
import com.team8.backend.global.exception.CustomException;
import com.team8.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class OrderFacadeIntegrationTest {

    @Autowired private OrderFacade orderFacade;
    @Autowired private OrderRepository orderRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private AuctionRepository auctionRepository;

    private Long orderId;
    private Long unauthorizedUserId;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        auctionRepository.deleteAll();
        userRepository.deleteAll();
        User seller = userRepository.save(User.create("s_f@t.com", "p", "S", "A"));
        User buyer = userRepository.save(User.create("b_f@t.com", "p", "B", "A"));
        User other = userRepository.save(User.create("o_f@t.com", "p", "O", "A"));

        unauthorizedUserId = other.getUserId();
        // Auction 필수 필드 보강
        Auction a = auctionRepository.save(Auction.builder().seller(seller).title("T")
                .startPrice(100L).currentPrice(100L).bidUnit(10).status(AuctionStatus.WON)
                .startedAt(LocalDateTime.now()).endedAt(LocalDateTime.now()).build());
        Order o = orderRepository.save(Order.builder().auction(a).bidder(buyer).seller(seller)
                .finalPrice(1000L).shippingAddress("A").createdAt(LocalDateTime.now()).build());
        orderId = o.getOrderId();
    }

    @Test
    @DisplayName("구매자가 아닌 유저가 결제를 시도하면 ACCESS_DENIED(403-1)가 발생한다")
    void payOrder_Unauthorized_Error() {
        assertThatThrownBy(() -> orderFacade.payOrder(orderId, unauthorizedUserId))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.ACCESS_DENIED.getMessage());
    }
    @Test
    @DisplayName("배송 정보 입력 시 '기본 배송지로 저장' 옵션이 true이면 유저 정보도 업데이트된다")
    void updateAddress_WithSaveAsDefault() {
        // Given
        OrderAddressRequest request = new OrderAddressRequest("새로운 집 주소", true);
        User buyer = userRepository.findByEmail("b_f@t.com").get();

        // When
        orderFacade.updateAddress(orderId, request, buyer.getUserId());

        // Then: 주문 배송지 및 유저 기본 주소 확인
        Order order = orderRepository.findById(orderId).get();
        User updatedUser = userRepository.findById(buyer.getUserId()).get();

        assertThat(order.getShippingAddress()).isEqualTo("새로운 집 주소");
        assertThat(updatedUser.getAddress()).isEqualTo("새로운 집 주소");
    }

    @Test
    @DisplayName("주문 참여자(구매자/판매자)가 아닌 제3자가 주문 상세를 조회하면 예외가 발생한다")
    void getOrder_Unauthorized_Error() {
        assertThatThrownBy(() -> orderFacade.getOrder(orderId, unauthorizedUserId))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.ACCESS_DENIED.getMessage());
    }
}
