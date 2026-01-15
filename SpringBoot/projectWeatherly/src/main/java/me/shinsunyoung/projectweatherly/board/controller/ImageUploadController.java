package me.shinsunyoung.projectweatherly.board.controller;

import lombok.RequiredArgsConstructor;
import me.shinsunyoung.projectweatherly.board.application.service.ImageUploadService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/community/images")
@RequiredArgsConstructor
public class ImageUploadController {

    private final ImageUploadService imageUploadService;

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            // 파일 크기 확인 (58.96KB = 60416 bytes)
            long maxSize = 60 * 1024; // 60KB
            if (file.getSize() > maxSize) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "이미지 크기는 60KB 이하여야 합니다.");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // 파일 확장자 확인
            String originalFilename = file.getOriginalFilename();
            if (originalFilename != null) {
                String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
                if (!extension.matches("jpg|jpeg|png|gif|bmp")) {
                    Map<String, String> errorResponse = new HashMap<>();
                    errorResponse.put("error", "지원하지 않는 파일 형식입니다.");
                    return ResponseEntity.badRequest().body(errorResponse);
                }
            }

            String imageUrl = imageUploadService.uploadImage(file);

            Map<String, String> response = new HashMap<>();
            response.put("imageUrl", imageUrl);
            response.put("message", "이미지 업로드 성공");

            return ResponseEntity.ok(response);
        } catch (IOException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "이미지 업로드 실패: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}