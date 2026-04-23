package com.team8.backend.global.init;

import com.team8.backend.domain.auction.entity.Auction;
import com.team8.backend.domain.auction.entity.AuctionStatus;
import com.team8.backend.domain.auction.repository.AuctionRepository;
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
@Profile("k6")
@RequiredArgsConstructor
public class K6DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final AuctionRepository auctionRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            return;  // 재시작 시 중복 시딩 방지 (ddl-auto: update 환경)
        }
        String encodedPw = passwordEncoder.encode("Test1234!");

        User seller = User.create("seller@k6.test", encodedPw, "K6Seller", "Seoul");
        userRepository.save(seller);

        for (int i = 1; i <= 10; i++) {
            userRepository.save(
                User.create("buyer" + i + "@k6.test", encodedPw, "Buyer" + i, "Seoul")
            );
        }

        LocalDateTime now = LocalDateTime.now();
        for (int i = 1; i <= 20; i++) {
            auctionRepository.save(Auction.builder()
                .seller(seller)
                .title("K6 Test Auction " + i)
                .description("load test")
                .startPrice(10_000L)
                .currentPrice(10_000L)
                .bidUnit(1_000)
                .status(AuctionStatus.ONGOING)
                .startedAt(now.minusHours(1))
                .endedAt(now.plusHours(48))
                .build());
        }
    }
}
