package com.team8.backend.domain.auction.service;

import com.team8.backend.domain.auction.dto.ImageFile;

public interface ImageService {
    String upload(ImageFile file, String domain); // MultipartFile -> ImageFile로 변경
    void delete(String imageUrl);
}