package com.team8.backend.domain.order.validator;

import com.team8.backend.domain.order.entity.Order;
import com.team8.backend.domain.order.entity.OrderStatus;
import com.team8.backend.domain.order.validator.address.AddressStatusStrategy;
import com.team8.backend.domain.order.validator.payment.PaymentAddressStrategy;
import com.team8.backend.domain.order.validator.payment.PaymentAlreadyPaidStrategy;
import com.team8.backend.domain.order.validator.payment.PaymentDeadlineStrategy;
import com.team8.backend.domain.order.validator.payment.PaymentStatusStrategy;
import com.team8.backend.domain.order.validator.shipping.ShippingAddressStrategy;
import com.team8.backend.domain.order.validator.shipping.ShippingDeadlineStrategy;
import com.team8.backend.domain.order.validator.shipping.ShippingDuplicateStrategy;
import com.team8.backend.domain.order.validator.shipping.ShippingStatusStrategy;
import com.team8.backend.global.exception.CustomException;
import com.team8.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OrderValidatorTest {
    
    @Test
    @DisplayName("결제 상태 검증: AWAITING_PAYMENT 상태이면 검증을 통과한다")
    void validate_PaymentStatus_Success() {
        PaymentStatusStrategy strategy = new PaymentStatusStrategy();
        Order order = mock(Order.class);
        when(order.getStatus()).thenReturn(OrderStatus.AWAITING_PAYMENT);

        assertThatCode(() -> strategy.validate(order)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("결제: 이미 결제된 상태인 경우 INVALID_ORDER_STATE 에러")
    void validate_PaymentStatus() {
        PaymentStatusStrategy strategy = new PaymentStatusStrategy();
        Order order = mock(Order.class);
        when(order.getStatus()).thenReturn(OrderStatus.PAID);
        assertThatThrownBy(() -> strategy.validate(order))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.INVALID_ORDER_STATE.getMessage());
    }

    @Test
    @DisplayName("중복 결제 검증: 이미 결제된 주문(isAlreadyPaid=true)이면 ALREADY_PAID 에러가 발생한다")
    void validate_PaymentAlreadyPaid_Fail() {
        PaymentAlreadyPaidStrategy strategy = new PaymentAlreadyPaidStrategy();
        Order order = mock(Order.class);
        when(order.isAlreadyPaid()).thenReturn(true);

        assertThatThrownBy(() -> strategy.validate(order))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.ALREADY_PAID.getMessage());
    }

    @Test
    @DisplayName("결제: 배송지 정보가 누락된 경우 SHIPPING_ADDRESS_MISSING 에러")
    void validate_PaymentAddress() {
        PaymentAddressStrategy strategy = new PaymentAddressStrategy();
        Order order = mock(Order.class);
        when(order.getShippingAddress()).thenReturn(null);
        assertThatThrownBy(() -> strategy.validate(order))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.SHIPPING_ADDRESS_MISSING.getMessage());
    }

    @Test
    @DisplayName("결제: 48시간 만료된 경우 PAYMENT_EXPIRED 에러")
    void validate_PaymentDeadline() {
        PaymentDeadlineStrategy strategy = new PaymentDeadlineStrategy();
        Order order = mock(Order.class);
        when(order.getPaymentDeadline()).thenReturn(LocalDateTime.now().minusSeconds(1));
        assertThatThrownBy(() -> strategy.validate(order))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.PAYMENT_EXPIRED.getMessage());
    }

    @Test
    @DisplayName("배송: 결제 대기 중일 때 송장 등록 시 INVALID_ORDER_STATE 에러")
    void validate_ShippingStatus() {
        ShippingStatusStrategy strategy = new ShippingStatusStrategy();
        Order order = mock(Order.class);
        when(order.getStatus()).thenReturn(OrderStatus.AWAITING_PAYMENT);
        assertThatThrownBy(() -> strategy.validate(order))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.INVALID_ORDER_STATE.getMessage());
    }

    @Test
    @DisplayName("배송: 이미 송장이 등록된 경우 TRACKING_ALREADY_EXISTS 에러")
    void validate_ShippingDuplicate() {
        ShippingDuplicateStrategy strategy = new ShippingDuplicateStrategy();
        Order order = mock(Order.class);
        when(order.getTrackingNumber()).thenReturn("EXIST_NUMBER");
        assertThatThrownBy(() -> strategy.validate(order))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.TRACKING_ALREADY_EXISTS.getMessage());
    }

    @Test
    @DisplayName("배송: 발송 기한(72H) 만료된 경우 DELIVERY_EXPIRED 에러")
    void validate_ShippingDeadline() {
        ShippingDeadlineStrategy strategy = new ShippingDeadlineStrategy();
        Order order = mock(Order.class);
        when(order.getDeliveryDeadline()).thenReturn(LocalDateTime.now().minusSeconds(1));
        assertThatThrownBy(() -> strategy.validate(order))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.DELIVERY_EXPIRED.getMessage());
    }
    
    @Test
    @DisplayName("배송 전략: 배송지 정보가 없는 상태에서 송장 등록 시 SHIPPING_ADDRESS_MISSING 발생")
    void validate_ShippingAddress_Error() {
        ShippingAddressStrategy strategy = new ShippingAddressStrategy();
        Order order = mock(Order.class);
        when(order.getShippingAddress()).thenReturn(null);

        assertThatThrownBy(() -> strategy.validate(order))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.SHIPPING_ADDRESS_MISSING.getMessage());
    }
    
    @Test
    @DisplayName("주소: 배송 시작 후 주소 수정 시 SHIPPING_ALREADY_STARTED 에러")
    void validate_AddressStatus() {
        AddressStatusStrategy strategy = new AddressStatusStrategy();
        Order order = mock(Order.class);
        when(order.isAddressUpdateEligible()).thenReturn(false);
        assertThatThrownBy(() -> strategy.validate(order))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.SHIPPING_ALREADY_STARTED.getMessage());
    }
}
