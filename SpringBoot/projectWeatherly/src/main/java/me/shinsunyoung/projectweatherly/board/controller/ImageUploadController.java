package me.shinsunyoung.projectweatherly.board.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.shinsunyoung.projectweatherly.board.service.ImageUploadService;
import me.shinsunyoung.projectweatherly.member.dto.UserSecurityDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequestMapping("/community/images")
@RequiredArgsConstructor
public class ImageUploadController {

    private final ImageUploadService imageUploadService;

    /**
     * 이미지 업로드 폼 페이지 (관리용)
     */
    @GetMapping("/upload")
    public String uploadForm(@AuthenticationPrincipal UserSecurityDTO user) {
        if (user == null || user.getUser() == null) {
            return "redirect:/login?redirect=/community/images/upload";
        }
        return "community/images/upload";
    }

    /**
     * 단일 이미지 업로드 처리 (템플릿 방식 - 폼 제출용)
     */
    @PostMapping("/upload")
    public String uploadImage(
            @AuthenticationPrincipal UserSecurityDTO user,
            @RequestParam("file") MultipartFile file,
            RedirectAttributes redirectAttributes,
            Model model) {

        if (user == null || user.getUser() == null) {
            return "redirect:/login";
        }

        try {
            validateImageFile(file);
            String imageUrl = imageUploadService.uploadImage(file);

            // 업로드 결과를 모델에 추가
            model.addAttribute("imageUrl", imageUrl);
            model.addAttribute("originalFilename", file.getOriginalFilename());
            model.addAttribute("size", file.getSize());
            model.addAttribute("message", "이미지 업로드 성공!");

            redirectAttributes.addFlashAttribute("imageUrl", imageUrl);
            redirectAttributes.addFlashAttribute("message", "이미지가 업로드되었습니다.");

            return "community/images/upload-success";

        } catch (IllegalArgumentException e) {
            log.warn("이미지 유효성 검사 실패: {}", e.getMessage());
            model.addAttribute("error", e.getMessage());
            return "community/images/upload";
        } catch (IOException e) {
            log.error("이미지 업로드 실패: {}", e.getMessage());
            model.addAttribute("error", "이미지 업로드 실패: " + e.getMessage());
            return "community/images/upload";
        }
    }

    /**
     * 단일 이미지 업로드 API (AJAX 용 - JSON 응답)
     */
    @PostMapping("/api/upload")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> uploadImageApi(
            @AuthenticationPrincipal UserSecurityDTO user,
            @RequestParam("file") MultipartFile file) {

        Map<String, Object> response = new HashMap<>();

        if (user == null || user.getUser() == null) {
            response.put("success", false);
            response.put("message", "로그인이 필요합니다.");
            response.put("errorCode", "UNAUTHORIZED");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        try {
            validateImageFile(file);
            String imageUrl = imageUploadService.uploadImage(file);

            response.put("success", true);
            response.put("imageUrl", imageUrl);
            response.put("message", "이미지 업로드 성공!");

            log.info("이미지 업로드 성공 - userId: {}, imageUrl: {}", user.getUser().getId(), imageUrl);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("이미지 유효성 검사 실패: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            response.put("errorCode", "VALIDATION_ERROR");
            return ResponseEntity.badRequest().body(response);
        } catch (IOException e) {
            log.error("이미지 업로드 실패: {}", e.getMessage());
            response.put("success", false);
            response.put("message", "이미지 업로드 실패: " + e.getMessage());
            response.put("errorCode", "UPLOAD_ERROR");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 다중 이미지 업로드 처리 (템플릿 방식)
     */
    @PostMapping("/upload-multiple")
    public String uploadMultipleImages(
            @AuthenticationPrincipal UserSecurityDTO user,
            @RequestParam("files") List<MultipartFile> files,
            RedirectAttributes redirectAttributes,
            Model model) {

        if (user == null || user.getUser() == null) {
            return "redirect:/login";
        }

        List<String> uploadedUrls = new ArrayList<>();
        List<String> errorMessages = new ArrayList<>();

        for (MultipartFile file : files) {
            try {
                validateImageFile(file);
                String imageUrl = imageUploadService.uploadImage(file);
                uploadedUrls.add(imageUrl);
                log.info("다중 이미지 업로드 성공 - userId: {}, fileName: {}",
                        user.getUser().getId(), file.getOriginalFilename());
            } catch (Exception e) {
                String errorMsg = file.getOriginalFilename() + ": " + e.getMessage();
                errorMessages.add(errorMsg);
                log.warn("다중 이미지 업로드 실패: {}", errorMsg);
            }
        }

        // 결과를 모델에 추가
        model.addAttribute("uploadedUrls", uploadedUrls);
        model.addAttribute("errorMessages", errorMessages);
        model.addAttribute("totalCount", files.size());
        model.addAttribute("successCount", uploadedUrls.size());

        if (!uploadedUrls.isEmpty()) {
            redirectAttributes.addFlashAttribute("uploadedUrls", uploadedUrls);
            redirectAttributes.addFlashAttribute("message",
                    uploadedUrls.size() + "개의 이미지가 업로드되었습니다.");
        }

        if (!errorMessages.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessages", errorMessages);
        }

        return "community/images/upload-multiple-result";
    }

    /**
     * 다중 이미지 업로드 API (AJAX 용 - JSON 응답)
     */
    @PostMapping("/api/upload-multiple")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> uploadMultipleImagesApi(
            @AuthenticationPrincipal UserSecurityDTO user,
            @RequestParam("files") List<MultipartFile> files) {

        Map<String, Object> response = new HashMap<>();

        if (user == null || user.getUser() == null) {
            response.put("success", false);
            response.put("message", "로그인이 필요합니다.");
            response.put("errorCode", "UNAUTHORIZED");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        List<String> uploadedUrls = new ArrayList<>();
        List<Map<String, String>> errors = new ArrayList<>();

        for (MultipartFile file : files) {
            try {
                validateImageFile(file);
                String imageUrl = imageUploadService.uploadImage(file);
                uploadedUrls.add(imageUrl);
            } catch (IllegalArgumentException | IOException e) {
                Map<String, String> error = new HashMap<>();
                error.put("fileName", file.getOriginalFilename());
                error.put("message", e.getMessage());
                errors.add(error);
            }
        }

        response.put("success", true);
        response.put("uploadedUrls", uploadedUrls);
        response.put("errors", errors);
        response.put("totalCount", files.size());
        response.put("successCount", uploadedUrls.size());
        response.put("message", uploadedUrls.size() + "개의 이미지가 업로드되었습니다.");

        log.info("다중 이미지 API 업로드 완료 - userId: {}, 성공: {}, 실패: {}",
                user.getUser().getId(), uploadedUrls.size(), errors.size());

        return ResponseEntity.ok(response);
    }

    /**
     * 이미지 관리 페이지
     */
    @GetMapping("/manage")
    public String manageImages(@AuthenticationPrincipal UserSecurityDTO user,
                               Model model) {
        if (user == null || user.getUser() == null) {
            return "redirect:/login";
        }

        // TODO: 사용자가 업로드한 이미지 목록 조회 구현
        // List<ImageInfo> userImages = imageUploadService.getUserImages(user.getUser().getId());
        // model.addAttribute("images", userImages);

        model.addAttribute("userId", user.getUser().getId());
        model.addAttribute("nickname", user.getUser().getNickname());

        return "community/images/manage";
    }

    /**
     * 이미지 삭제 처리 (템플릿 방식)
     */
    @PostMapping("/delete")
    public String deleteImage(
            @AuthenticationPrincipal UserSecurityDTO user,
            @RequestParam String imageUrl,
            RedirectAttributes redirectAttributes) {

        if (user == null || user.getUser() == null) {
            return "redirect:/login";
        }

        try {
            // TODO: 이미지 삭제 서비스 구현
            // imageUploadService.deleteImage(user.getUser().getId(), imageUrl);
            redirectAttributes.addFlashAttribute("message", "이미지가 삭제되었습니다.");
        } catch (Exception e) {
            log.error("이미지 삭제 실패: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "이미지 삭제 실패: " + e.getMessage());
        }

        return "redirect:/community/images/manage";
    }

    /**
     * 이미지 삭제 API (AJAX 용)
     */
    @PostMapping("/api/delete")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteImageApi(
            @AuthenticationPrincipal UserSecurityDTO user,
            @RequestParam String imageUrl) {

        Map<String, Object> response = new HashMap<>();

        if (user == null || user.getUser() == null) {
            response.put("success", false);
            response.put("message", "로그인이 필요합니다.");
            response.put("errorCode", "UNAUTHORIZED");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        try {
            // TODO: 이미지 삭제 서비스 구현
            // imageUploadService.deleteImage(user.getUser().getId(), imageUrl);

            response.put("success", true);
            response.put("message", "이미지가 삭제되었습니다.");

            log.info("이미지 삭제 성공 - userId: {}, imageUrl: {}", user.getUser().getId(), imageUrl);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("이미지 삭제 실패: {}", e.getMessage());
            response.put("success", false);
            response.put("message", "이미지 삭제 실패: " + e.getMessage());
            response.put("errorCode", "DELETE_ERROR");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 이미지 업로드 상태 확인 API
     */
    @GetMapping("/api/status")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getUploadStatus(
            @AuthenticationPrincipal UserSecurityDTO user) {

        Map<String, Object> response = new HashMap<>();

        if (user == null || user.getUser() == null) {
            response.put("success", false);
            response.put("message", "로그인이 필요합니다.");
            response.put("errorCode", "UNAUTHORIZED");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        try {
            // TODO: 업로드 상태 정보 조회
            response.put("success", true);
            response.put("maxFileSize", "60KB");
            response.put("allowedExtensions", new String[]{"jpg", "jpeg", "png", "gif", "bmp", "webp"});
            response.put("message", "이미지 업로드 서비스 정상 작동 중");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("업로드 상태 조회 실패: {}", e.getMessage());
            response.put("success", false);
            response.put("message", "서비스 상태를 확인할 수 없습니다.");
            response.put("errorCode", "SERVICE_ERROR");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 이미지 파일 유효성 검사
     */
    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어 있습니다.");
        }

        // 파일 크기 확인 (60KB = 61440 bytes)
        long maxSize = 60 * 1024; // 60KB
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("이미지 크기는 60KB 이하여야 합니다. 현재: " +
                    (file.getSize() / 1024) + "KB");
        }

        // 파일 확장자 확인
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null) {
            String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
            if (!extension.matches("jpg|jpeg|png|gif|bmp|webp")) {
                throw new IllegalArgumentException("지원하지 않는 파일 형식입니다. (jpg, jpeg, png, gif, bmp, webp만 가능)");
            }
        } else {
            throw new IllegalArgumentException("파일 이름이 없습니다.");
        }
    }
}