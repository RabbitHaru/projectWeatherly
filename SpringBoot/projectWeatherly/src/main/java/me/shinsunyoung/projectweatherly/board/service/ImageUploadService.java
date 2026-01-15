package me.shinsunyoung.projectweatherly.board.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@Slf4j
public class ImageUploadService {

    // 하드코딩으로 프로퍼티 문제 해결
    private static final String UPLOAD_DIR = "uploads";

    @PostConstruct
    public void init() {
        try {
            // 디렉토리 생성
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                log.info("이미지 업로드 디렉토리 생성됨: {}", uploadPath.toAbsolutePath());
            }
        } catch (IOException e) {
            log.error("이미지 업로드 디렉토리 생성 실패: {}", e.getMessage());
        }
    }

    public String uploadImage(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어 있습니다.");
        }

        // 파일 검증
        validateImageFile(file);

        // 원본 파일명에서 확장자 추출
        String originalFilename = file.getOriginalFilename();
        String fileExtension = "";

        if (originalFilename != null && originalFilename.contains(".")) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        // UUID로 파일명 생성
        String savedFilename = UUID.randomUUID().toString() + fileExtension;

        // 디렉토리 확인
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // 파일 저장
        Path filePath = uploadPath.resolve(savedFilename);
        Files.copy(file.getInputStream(), filePath);

        // 이미지 URL 반환 (상대 경로)
        return "/" + UPLOAD_DIR + "/" + savedFilename;
    }

    private void validateImageFile(MultipartFile file) {
        // 파일 크기 확인 (60KB = 61440 bytes)
        long maxSize = 60 * 1024;
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("이미지 크기는 60KB 이하여야 합니다.");
        }

        // 파일 확장자 확인
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null) {
            String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
            if (!extension.matches("jpg|jpeg|png|gif|bmp|webp")) {
                throw new IllegalArgumentException("지원하지 않는 파일 형식입니다. (jpg, jpeg, png, gif, bmp, webp만 가능)");
            }
        }
    }

    public void deleteImage(String imageUrl) throws IOException {
        if (imageUrl != null && imageUrl.startsWith("/" + UPLOAD_DIR + "/")) {
            String filename = imageUrl.substring(("/" + UPLOAD_DIR + "/").length());
            Path filePath = Paths.get(UPLOAD_DIR, filename);
            Files.deleteIfExists(filePath);
        }
    }
}