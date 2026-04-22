package com.team8.backend.domain.order.facade;

import com.team8.backend.domain.auction.entity.Auction;
import com.team8.backend.domain.auction.entity.AuctionStatus;
import com.team8.backend.domain.auction.repository.AuctionRepository;
import com.team8.backend.domain.bid.repository.BidRepository;
import com.team8.backend.domain.order.dto.request.OrderAddressRequest;
import com.team8.backend.domain.order.dto.request.OrderTrackingRequest;
import com.team8.backend.domain.order.entity.Order;
import com.team8.backend.domain.order.entity.OrderStatus;
import com.team8.backend.domain.order.repository.OrderRepository;
import com.team8.backend.domain.order.service.OrderService;
import com.team8.backend.domain.user.entity.User;
import com.team8.backend.domain.user.entity.UserPermission;
import com.team8.backend.domain.user.repository.UserRepository;
import com.team8.backend.global.exception.CustomException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class OrderConcurrencyTest2 {

//    @Autowired OrderFacade orderFacade;
//    @Autowired OrderService orderService;
//    @Autowired OrderRepository orderRepository;
//    @Autowired AuctionRepository auctionRepository;
//    @Autowired UserRepository userRepository;
//    @Autowired BidRepository bidRepository;
//
//    User seller;
//    User buyer;
//    Auction auction;
//    Order order;
//
//    @BeforeEach
//    void setUp() {
//        seller = userRepository.save(User.create("seller@order-test.com", "pw", "판매자", "서울"));
//        seller.grantPermission(UserPermission.SELLER);
//        userRepository.save(seller);
//
//        buyer = userRepository.save(User.create("buyer@order-test.com", "pw", "구매자", "부산 해운대구 센텀시티 123"));
//
//        auction = auctionRepository.save(Auction.builder()
//                .seller(seller)
//                .title("낙찰된 경매")
//                .description("설명")
//                .imageUrl("test.jpg")
//                .startPrice(10_000L)
//                .currentPrice(50_000L)
//                .lastBidder(buyer)
//                .bidUnit(1_000)
//                .status(AuctionStatus.WON)
//                .startedAt(LocalDateTime.now().minusDays(2))
//                .endedAt(LocalDateTime.now().minusDays(1))
//                .build());
//
//        order = orderRepository.save(Order.builder()
//                .auction(auction)
//                .bidder(buyer)
//                .seller(seller)
//                .finalPrice(50_000L)
//                .shippingAddress("부산 해운대구 센텀시티 123")
//                .createdAt(LocalDateTime.now())
//                .build());
//    }
//
//    @AfterEach
//    void tearDown() {
//        bidRepository.deleteAll();
//        orderRepository.deleteAll();
//        auctionRepository.deleteAll();
//        userRepository.deleteAll();
//    }
//
//    /**
//     * [버그 재현] 동일한 주문에 대해 결제 요청이 동시에 들어올 때 처리 문제.
//     *
//     * 현재 동작:
//     *   - OrderFacade.payOrder()에 OptimisticLockingFailureException에 대한 catch가 없다.
//     *   - 커밋 시점에 충돌이 발생하면 ObjectOptimisticLockingFailureException이 그대로 노출된다.
//     *   - 클라이언트는 500 에러를 받는다.
//     *
//     * 기대 동작:
//     *   - 충돌한 결제 요청은 CustomException(ALREADY_PAID 또는 CONFLICT)을 반환해야 한다.
//     */
//    @Test
//    @DisplayName("[버그] 동시 결제 요청 시 실패 요청은 CustomException이어야 한다")
//    void 동시_결제_요청_시_실패_요청은_CustomException이어야_한다() throws InterruptedException {
//        int threadCount = 2;
//        CountDownLatch startLatch = new CountDownLatch(1);
//        CountDownLatch doneLatch = new CountDownLatch(threadCount);
//        List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());
//        AtomicInteger successCount = new AtomicInteger(0);
//
//        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
//
//        Runnable payTask = () -> {
//            try {
//                startLatch.await();
//                orderFacade.payOrder(order.getOrderId(), buyer.getUserId());
//                successCount.incrementAndGet();
//            } catch (Exception e) {
//                exceptions.add(e);
//            } finally {
//                doneLatch.countDown();
//            }
//        };
//
//        executor.submit(payTask);
//        executor.submit(payTask);
//
//        startLatch.countDown();
//        doneLatch.await(10, TimeUnit.SECONDS);
//        executor.shutdown();
//
//        // 동시 결제 충돌이 발생했다면 하나만 성공해야 한다
//        if (!exceptions.isEmpty()) {
//            assertThat(successCount.get()).isEqualTo(1);
//
//            // [현재 실패] ObjectOptimisticLockingFailureException이 그대로 노출됨
//            // [수정 후 통과] CustomException(ALREADY_PAID 또는 CONFLICT)이 반환되어야 함
//            assertThat(exceptions.get(0))
//                    .as("동시 결제 충돌 시 CustomException을 반환해야 합니다. " +
//                            "현재: %s", exceptions.get(0).getClass().getSimpleName())
//                    .isInstanceOf(CustomException.class);
//        }
//    }
//
//    /**
//     * [버그 재현] 배송지 수정과 결제가 동시에 발생할 때 처리 문제.
//     *
//     * 현재 동작:
//     *   - updateAddress와 payOrder 모두 getOrderWithLockOrThrow(OPTIMISTIC)을 사용한다.
//     *   - 두 요청이 같은 version을 읽고 동시에 커밋하면 한 쪽이 OptimisticLockingFailureException을 받는다.
//     *   - 해당 예외에 대한 처리가 없어 클라이언트는 500 에러를 받는다.
//     *
//     * 현실적 시나리오:
//     *   - 구매자가 배송지를 수정하자마자 결제를 누르는 경우 (두 요청이 거의 동시에 서버에 도달)
//     *
//     * 기대 동작:
//     *   - 충돌이 발생하면 의미 있는 CustomException을 반환하거나 재시도해야 한다.
//     */
//    @Test
//    @DisplayName("[버그] 배송지 수정과 결제 동시 발생 시 실패 요청은 CustomException이어야 한다")
//    void 배송지_수정과_결제_동시_발생_시_실패_요청은_CustomException이어야_한다() throws InterruptedException {
//        int threadCount = 2;
//        CountDownLatch startLatch = new CountDownLatch(1);
//        CountDownLatch doneLatch = new CountDownLatch(threadCount);
//        List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());
//
//        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
//
//        executor.submit(() -> {
//            try {
//                startLatch.await();
//                orderFacade.updateAddress(
//                        order.getOrderId(),
//                        new OrderAddressRequest("서울 강남구 테헤란로 123", false),
//                        buyer.getUserId()
//                );
//            } catch (Exception e) {
//                exceptions.add(e);
//            } finally {
//                doneLatch.countDown();
//            }
//        });
//
//        executor.submit(() -> {
//            try {
//                startLatch.await();
//                orderFacade.payOrder(order.getOrderId(), buyer.getUserId());
//            } catch (Exception e) {
//                exceptions.add(e);
//            } finally {
//                doneLatch.countDown();
//            }
//        });
//
//        startLatch.countDown();
//        doneLatch.await(10, TimeUnit.SECONDS);
//        executor.shutdown();
//
//        // 충돌이 발생한 경우에만 검증
//        if (!exceptions.isEmpty()) {
//            // [현재 실패] ObjectOptimisticLockingFailureException이 그대로 노출됨
//            // [수정 후 통과] CustomException이 반환되어야 함
//            assertThat(exceptions.get(0))
//                    .as("동시 수정 충돌 시 CustomException을 반환해야 합니다. " +
//                            "현재: %s", exceptions.get(0).getClass().getSimpleName())
//                    .isInstanceOf(CustomException.class);
//        }
//    }
//
//    /**
//     * [버그 재현] 결제 마감 경계에서 결제와 notPaidOrder 스케줄러가 동시에 실행될 때
//     * payOrder에서 OptimisticLockingFailureException이 처리되지 않는 문제.
//     *
//     * 재현 시나리오:
//     *   1. 결제 마감 직전에 사용자가 결제 시작 → payOrder가 AWAITING_PAYMENT 확인 후 진행
//     *   2. 동시에 스케줄러가 notPaidOrder 실행 → NOT_PAID로 변경, version 증가
//     *   3. payOrder 커밋 시 version 불일치 → OptimisticLockingFailureException
//     *   4. OrderFacade에 catch 없음 → 클라이언트는 500 에러
//     *
//     * 기대 동작:
//     *   - 결제 또는 스케줄러 중 하나가 성공해야 하고
//     *   - 실패 측은 CustomException을 받아야 한다 (500이 아닌 의미 있는 에러)
//     */
//    @Test
//    @DisplayName("[버그] 결제 마감 경계에서 notPaidOrder와 payOrder 동시 실행 시 실패 측은 CustomException이어야 한다")
//    void 결제_마감_경계에서_notPaidOrder와_payOrder_동시_실행_시_실패_측은_CustomException이어야_한다()
//            throws InterruptedException {
//        // paymentDeadline을 과거로 만들기 위해 createdAt을 49시간 전으로 설정
//        // paymentDeadline = createdAt + 48h = 1시간 전
//        Order expiredOrder = orderRepository.save(Order.builder()
//                .auction(auction)
//                .bidder(buyer)
//                .seller(seller)
//                .finalPrice(50_000L)
//                .shippingAddress("부산 해운대구 센텀시티 123")
//                .createdAt(LocalDateTime.now().minusHours(49))
//                .build());
//
//        CountDownLatch startLatch = new CountDownLatch(1);
//        CountDownLatch doneLatch = new CountDownLatch(2);
//        List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());
//        AtomicInteger successCount = new AtomicInteger(0);
//
//        ExecutorService executor = Executors.newFixedThreadPool(2);
//
//        // Thread A: 스케줄러 시뮬레이션 - 결제 마감 처리
//        executor.submit(() -> {
//            try {
//                startLatch.await();
//                orderService.notPaidOrder(expiredOrder.getOrderId());
//                successCount.incrementAndGet();
//            } catch (Exception e) {
//                exceptions.add(e);
//            } finally {
//                doneLatch.countDown();
//            }
//        });
//
//        // Thread B: 사용자 결제 시도 (마감이 지났지만 동시에 실행)
//        executor.submit(() -> {
//            try {
//                startLatch.await();
//                orderFacade.payOrder(expiredOrder.getOrderId(), buyer.getUserId());
//                successCount.incrementAndGet();
//            } catch (Exception e) {
//                exceptions.add(e);
//            } finally {
//                doneLatch.countDown();
//            }
//        });
//
//        startLatch.countDown();
//        doneLatch.await(10, TimeUnit.SECONDS);
//        executor.shutdown();
//
//        // 충돌이 발생한 경우 예외 타입 검증
//        if (!exceptions.isEmpty()) {
//            // [현재 실패] payOrder에서 ObjectOptimisticLockingFailureException이 그대로 노출됨
//            // [수정 후 통과] CustomException이 반환되어야 함
//            assertThat(exceptions.get(0))
//                    .as("결제 마감 경계 충돌 시 CustomException을 반환해야 합니다. " +
//                            "현재: %s", exceptions.get(0).getClass().getSimpleName())
//                    .isInstanceOf(CustomException.class);
//        }
//    }
//
//    /**
//     * [버그 재현] 배송 등록과 notDeliveredOrder 스케줄러가 동시에 실행될 때
//     * registerTracking에서 OptimisticLockingFailureException이 처리되지 않는 문제.
//     *
//     * 재현 시나리오:
//     *   1. 발송 마감 직전에 판매자가 배송 등록 시작
//     *   2. 동시에 스케줄러가 notDeliveredOrder 실행 → NOT_DELIVERED로 변경, version 증가
//     *   3. registerTracking 커밋 시 version 불일치 → OptimisticLockingFailureException
//     *   4. OrderFacade에 catch 없음 → 클라이언트는 500 에러
//     */
//    @Test
//    @DisplayName("[버그] 배송 마감 경계에서 notDeliveredOrder와 registerTracking 동시 실행 시 실패 측은 CustomException이어야 한다")
//    void 배송_마감_경계에서_notDeliveredOrder와_registerTracking_동시_실행_시_실패_측은_CustomException이어야_한다()
//            throws InterruptedException {
//        // PAID 상태이고 deliveryDeadline이 지난 주문 만들기
//        // 1. 먼저 결제 처리 (AWAITING_PAYMENT → PAID)
//        orderFacade.payOrder(order.getOrderId(), buyer.getUserId());
//
//        // 2. deliveryDeadline이 과거가 되도록 직접 notDeliveredOrder를 호출하기 전
//        //    deliveryDeadline = paidAt + 72h 인데, paidAt을 73시간 전으로 만들어야 함
//        //    → 별도 order 생성으로 시뮬레이션
//        Order paidOrder = orderRepository.findById(order.getOrderId()).orElseThrow();
//
//        // deliveryDeadline이 지난 상태를 만들기 위해 새 주문을 직접 PAID 상태로 만들기는 복잡하므로
//        // PAID 상태인 현재 주문으로 동시성 테스트 진행
//        CountDownLatch startLatch = new CountDownLatch(1);
//        CountDownLatch doneLatch = new CountDownLatch(2);
//        List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());
//
//        ExecutorService executor = Executors.newFixedThreadPool(2);
//
//        // Thread A: 스케줄러 시뮬레이션 - 미발송 처리
//        executor.submit(() -> {
//            try {
//                startLatch.await();
//                orderService.notDeliveredOrder(paidOrder.getOrderId());
//            } catch (Exception e) {
//                exceptions.add(e);
//            } finally {
//                doneLatch.countDown();
//            }
//        });
//
//        // Thread B: 판매자 배송 등록
//        executor.submit(() -> {
//            try {
//                startLatch.await();
//                orderFacade.registerTracking(
//                        paidOrder.getOrderId(),
//                        new OrderTrackingRequest("123-456-789"),
//                        seller.getUserId()
//                );
//            } catch (Exception e) {
//                exceptions.add(e);
//            } finally {
//                doneLatch.countDown();
//            }
//        });
//
//        startLatch.countDown();
//        doneLatch.await(10, TimeUnit.SECONDS);
//        executor.shutdown();
//
//        // 충돌이 발생한 경우 예외 타입 검증
//        if (!exceptions.isEmpty()) {
//            // [현재 실패] ObjectOptimisticLockingFailureException이 그대로 노출됨
//            // [수정 후 통과] CustomException이 반환되어야 함
//            assertThat(exceptions.get(0))
//                    .as("배송 마감 경계 충돌 시 CustomException을 반환해야 합니다. " +
//                            "현재: %s", exceptions.get(0).getClass().getSimpleName())
//                    .isInstanceOf(CustomException.class);
//        }
//    }
//
//    /**
//     * [정상] 이미 결제된 주문에 notPaidOrder를 호출해도 PAID 상태를 유지해야 한다.
//     * (markNotPaid 내부 guard: status != AWAITING_PAYMENT → 조기 리턴)
//     */
//    @Test
//    @DisplayName("[정상] 이미 결제된 주문에 notPaidOrder 호출 시 PAID 상태를 유지해야 한다")
//    void 이미_결제된_주문에_notPaidOrder_호출_시_PAID_상태를_유지해야_한다() {
//        // 결제 처리
//        orderFacade.payOrder(order.getOrderId(), buyer.getUserId());
//
//        // 스케줄러가 결제된 주문에 notPaidOrder 호출 (잘못된 호출 시뮬레이션)
//        orderService.notPaidOrder(order.getOrderId());
//
//        Order result = orderRepository.findById(order.getOrderId()).orElseThrow();
//        assertThat(result.getStatus())
//                .as("이미 결제된 주문은 notPaidOrder 호출 후에도 PAID여야 합니다.")
//                .isEqualTo(OrderStatus.PAID);
//    }
//
//    /**
//     * [정상] DELIVERING 상태 주문에 notDeliveredOrder를 호출해도 DELIVERING 상태를 유지해야 한다.
//     * (deliveryDeadline이 아직 미래인 경우 markNotDelivered 내부 guard 동작)
//     */
//    @Test
//    @DisplayName("[정상] 배송 마감 전 배송 등록된 주문에 notDeliveredOrder 호출 시 DELIVERING 상태를 유지해야 한다")
//    void 배송_마감_전_배송_등록된_주문에_notDeliveredOrder_호출_시_DELIVERING_상태를_유지해야_한다() {
//        // 결제 → 배송 등록
//        orderFacade.payOrder(order.getOrderId(), buyer.getUserId());
//        orderFacade.registerTracking(
//                order.getOrderId(),
//                new OrderTrackingRequest("123-456-789"),
//                seller.getUserId()
//        );
//
//        // 스케줄러가 deliveryDeadline 이전에 notDeliveredOrder 호출 (잘못된 호출 시뮬레이션)
//        orderService.notDeliveredOrder(order.getOrderId());
//
//        Order result = orderRepository.findById(order.getOrderId()).orElseThrow();
//        assertThat(result.getStatus())
//                .as("deliveryDeadline 이전에 배송 등록된 주문은 notDeliveredOrder 호출 후에도 DELIVERING이어야 합니다.")
//                .isEqualTo(OrderStatus.DELIVERING);
//    }
}
