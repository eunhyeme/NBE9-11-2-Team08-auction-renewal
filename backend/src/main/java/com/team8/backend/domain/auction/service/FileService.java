package com.team8.backend.domain.auction.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class FileService {

    public String saveFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }

        // 1. 파일명 중복 방지를 위한 UUID 생성
        String originalFilename = file.getOriginalFilename();
        String uuid = UUID.randomUUID().toString();
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String savedName = uuid + extension;

        // 2. 프로젝트 루트 기준의 'uploads' 폴더 절대 경로 확보
        // Paths.get("uploads").toAbsolutePath()를 써서 톰캣 임시 폴더가 아닌 실제 프로젝트 폴더를 조준합니다.
        Path uploadPath = Paths.get("uploads").toAbsolutePath();
        File uploadDir = uploadPath.toFile();

        // 혹시라도 폴더가 없으면 자동으로 생성하는 안전 장치를 추가합니다.
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }

        // 3. 파일 저장 실행
        File targetFile = new File(uploadDir, savedName);
        file.transferTo(targetFile);

        // 4. DB에 저장될 가상 경로 반환
        return "/uploads/" + savedName;
    }

    public void deleteFile(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return;
        }

        String fileName = imageUrl.replace("/uploads/", "");
        Path filePath = Paths.get("uploads").toAbsolutePath().resolve(fileName);
        File file = filePath.toFile();

        if (file.exists()) {
            boolean deleted = file.delete();
            if (!deleted) {
                // 삭제 실패 시 런타임 예외를 던져 시스템에 알립니다.
                throw new RuntimeException("파일 삭제에 실패했습니다: " + filePath);
            }
        }
    }
}