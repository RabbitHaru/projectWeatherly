package me.shinsunyoung.projectweatherly.board.controller;

import lombok.RequiredArgsConstructor;
import me.shinsunyoung.projectweatherly.board.service.ImageUploadService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/community/images")
@RequiredArgsConstructor
public class ImageUploadController {

    private final ImageUploadService imageUploadService;

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            return handleSingleImageUpload(file);
        } catch (IOException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "이미지 업로드 실패: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @PostMapping("/upload-multiple")
    public ResponseEntity<Map<String, Object>> uploadMultipleImages(@RequestParam("files") List<MultipartFile> files) {
        try {
            return handleMultipleImageUpload(files);
        } catch (IOException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "이미지 업로드 실패: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    private ResponseEntity<Map<String, Object>> handleSingleImageUpload(MultipartFile file) throws IOException {
        validateImageFile(file);

        String imageUrl = imageUploadService.uploadImage(file);

        Map<String, Object> response = new HashMap<>();
        response.put("imageUrl", imageUrl);
        response.put("message", "이미지 업로드 성공");
        response.put("originalFilename", file.getOriginalFilename());
        response.put("size", file.getSize());

        return ResponseEntity.ok(response);
    }

    private ResponseEntity<Map<String, Object>> handleMultipleImageUpload(List<MultipartFile> files) throws IOException {
        List<Map<String, Object>> uploadedImages = new ArrayList<>();
        List<String> errorMessages = new ArrayList<>();

        for (MultipartFile file : files) {
            try {
                validateImageFile(file);

                String imageUrl = imageUploadService.uploadImage(file);

                Map<String, Object> imageInfo = new HashMap<>();
                imageInfo.put("imageUrl", imageUrl);
                imageInfo.put("originalFilename", file.getOriginalFilename());
                imageInfo.put("size", file.getSize());
                imageInfo.put("success", true);

                uploadedImages.add(imageInfo);
            } catch (Exception e) {
                Map<String, Object> errorInfo = new HashMap<>();
                errorInfo.put("originalFilename", file.getOriginalFilename());
                errorInfo.put("error", e.getMessage());
                errorInfo.put("success", false);

                uploadedImages.add(errorInfo);
                errorMessages.add(file.getOriginalFilename() + ": " + e.getMessage());
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("uploadedImages", uploadedImages);
        response.put("totalCount", files.size());
        response.put("successCount", uploadedImages.stream().filter(img -> (Boolean) img.get("success")).count());
        response.put("failureCount", uploadedImages.stream().filter(img -> !(Boolean) img.get("success")).count());

        if (!errorMessages.isEmpty()) {
            response.put("errorMessages", errorMessages);
        }

        return ResponseEntity.ok(response);
    }

    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어 있습니다.");
        }

        // 파일 크기 확인 (60KB = 61440 bytes)
        long maxSize = 60 * 1024; // 60KB
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("이미지 크기는 60KB 이하여야 합니다.");
        }

        // 파일 확장자 확인
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null) {
            String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
            if (!extension.matches("jpg|jpeg|png|gif|bmp")) {
                throw new IllegalArgumentException("지원하지 않는 파일 형식입니다. (jpg, jpeg, png, gif, bmp만 가능)");
            }
        }
    }
}