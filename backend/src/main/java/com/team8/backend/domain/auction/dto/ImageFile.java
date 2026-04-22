package com.team8.backend.domain.auction.dto;

import java.io.InputStream;

/**
 * 서비스 계층에서 MultipartFile 의존성을 제거하기 위한 추상화된 이미지 DTO
 */
public record ImageFile(
        InputStream inputStream,
        String originalFilename,
        String contentType,
        long size
) {
}