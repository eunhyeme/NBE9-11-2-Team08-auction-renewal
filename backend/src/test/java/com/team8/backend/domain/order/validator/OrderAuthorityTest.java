package com.team8.backend.domain.order.validator.authority;

import com.team8.backend.domain.order.entity.Order;
import com.team8.backend.global.exception.CustomException;
import com.team8.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OrderAuthorityTest {

    private final BuyerAuthorityValidator buyerValidator = new BuyerAuthorityValidator();
    private final SellerAuthorityValidator sellerValidator = new SellerAuthorityValidator();

    @Test
    @DisplayName("구매자 권한: 구매자가 아닌 유저가 접근하면 ACCESS_DENIED 발생")
    void validate_BuyerAuthority_Fail() {
        Order order = mock(Order.class);
        Long userId = 1L;
        when(order.isBuyer(userId)).thenReturn(false);

        assertThatThrownBy(() -> buyerValidator.validate(order, userId))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.ACCESS_DENIED.getMessage());
    }

    @Test
    @DisplayName("판매자 권한: 판매자가 아닌 유저가 접근하면 ACCESS_DENIED 발생")
    void validate_SellerAuthority_Fail() {
        Order order = mock(Order.class);
        Long userId = 2L;
        when(order.isSeller(userId)).thenReturn(false);

        assertThatThrownBy(() -> sellerValidator.validate(order, userId))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.ACCESS_DENIED.getMessage());
    }
    @Test
    @DisplayName("참여자 권한: 구매자도 판매자도 아닌 유저가 접근하면 ACCESS_DENIED 발생")
    void validate_ParticipantAuthority_Fail() {
        OrderParticipantAuthorityValidator validator = new OrderParticipantAuthorityValidator();
        Order order = mock(Order.class);
        Long userId = 3L;
        when(order.isParticipant(userId)).thenReturn(false);

        assertThatThrownBy(() -> validator.validate(order, userId))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.ACCESS_DENIED.getMessage());
    }
}
