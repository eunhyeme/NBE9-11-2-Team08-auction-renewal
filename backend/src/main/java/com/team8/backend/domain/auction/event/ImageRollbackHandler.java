package com.team8.backend.domain.auction.event;

import com.team8.backend.domain.auction.service.ImageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ImageRollbackHandler {
    private final ImageService imageService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    public void handleRollback(ImageUploadEvent event) {
        log.info("DB 롤백 감지. 고아 파일 삭제 시작: {}", event.imageUrl());
        try {
            imageService.delete(event.imageUrl());
        } catch (Exception e) {
            log.error("고아 파일 삭제 중 에러 발생", e);
        }
    }
}