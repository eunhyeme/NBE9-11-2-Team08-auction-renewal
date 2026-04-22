package com.team8.backend.domain.auction.service;

import com.team8.backend.domain.auction.dto.ImageFile;
import com.team8.backend.global.exception.CustomException;
import com.team8.backend.global.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@Primary // 나중에 S3 구현체가 나오면 이 어노테이션만 옮기면 됩니다.
public class LocalImageService implements ImageService {

    @Value("${file.upload-dir:uploads}") // application.yml에 설정이 없으면 uploads 폴더 사용
    private String uploadDir;

    @Override
    public String upload(ImageFile file, String domain) {
        if (file == null || file.inputStream() == null) return null;

        try {
            String fileName = UUID.randomUUID() + "_" + file.originalFilename();
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();

            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // 💡 MultipartFile.getInputStream() 대신 DTO의 inputStream() 사용
            Files.copy(file.inputStream(), uploadPath.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);

            return "/images/" + fileName;
        } catch (IOException e) {
            throw new RuntimeException("이미지 저장 실패!", e);
        }
    }

    @Override
    public void delete(String imageUrl) {
        if (imageUrl == null || !imageUrl.startsWith("/images/")) return;

        try {
            String fileName = imageUrl.replace("/images/", "");
            Path filePath = Paths.get(uploadDir).resolve(fileName).toAbsolutePath().normalize();

            // 파일이 존재할 때만 삭제 시도
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            // 💡 프로젝트 커스텀 익셉션 적용
            throw new CustomException(ErrorCode.IMAGE_DELETE_FAILED);
        }
    }
}