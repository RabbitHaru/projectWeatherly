package me.shinsunyoung.projectweatherly.board.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImageUploadService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    public String uploadImage(MultipartFile file) throws IOException {
        // 디렉토리 생성
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // 원본 파일명에서 확장자 추출
        String originalFilename = file.getOriginalFilename();
        String fileExtension = "";

        if (originalFilename != null && originalFilename.contains(".")) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        // UUID로 파일명 생성
        String savedFilename = UUID.randomUUID().toString() + fileExtension;

        // 파일 저장
        Path filePath = uploadPath.resolve(savedFilename);
        Files.copy(file.getInputStream(), filePath);

        // 이미지 URL 반환
        return "/uploads/" + savedFilename;
    }

    public void deleteImage(String imageUrl) throws IOException {
        if (imageUrl != null && imageUrl.startsWith("/uploads/")) {
            String filename = imageUrl.substring("/uploads/".length());
            Path filePath = Paths.get(uploadDir, filename);
            Files.deleteIfExists(filePath);
        }
    }
}