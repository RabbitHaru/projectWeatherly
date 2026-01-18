package me.shinsunyoung.projectweatherly.board.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.shinsunyoung.projectweatherly.board.service.ImageUploadService;
import me.shinsunyoung.projectweatherly.member.dto.UserSecurityDTO;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Controller
@RequestMapping("/community/images")
@RequiredArgsConstructor
public class ImageUploadController {

    private final ImageUploadService imageUploadService;

    /**
     * 이미지 업로드 폼 페이지
     */
    @GetMapping("/upload")
    public String uploadForm(@AuthenticationPrincipal UserSecurityDTO user,
                             Model model) {
        if (user == null || user.getUser() == null) {
            return "redirect:/login";
        }

        model.addAttribute("userId", user.getUser().getId());
        model.addAttribute("nickname", user.getUser().getNickname());
        return "community/images/upload";
    }

    /**
     * 단일 이미지 업로드 처리
     */
    @PostMapping("/upload")
    public String uploadImage(
            @AuthenticationPrincipal UserSecurityDTO user,
            @RequestParam("file") MultipartFile file,
            RedirectAttributes redirectAttributes) {

        if (user == null || user.getUser() == null) {
            return "redirect:/login";
        }

        try {
            validateImageFile(file);
            String filename = imageUploadService.uploadImage(file);

            redirectAttributes.addFlashAttribute("message", "이미지 업로드 성공!");
            redirectAttributes.addFlashAttribute("filename", filename);
            redirectAttributes.addFlashAttribute("originalFilename", file.getOriginalFilename());

            return "redirect:/community/images/upload-success";

        } catch (IllegalArgumentException e) {
            log.warn("이미지 유효성 검사 실패: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/community/images/upload";
        } catch (IOException e) {
            log.error("이미지 업로드 실패: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "이미지 업로드 실패: " + e.getMessage());
            return "redirect:/community/images/upload";
        }
    }

    /**
     * 다중 이미지 업로드 처리
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

        List<String> uploadedFilenames = new ArrayList<>();
        List<String> errorMessages = new ArrayList<>();

        for (MultipartFile file : files) {
            try {
                validateImageFile(file);
                String filename = imageUploadService.uploadImage(file);
                uploadedFilenames.add(filename);
            } catch (Exception e) {
                String errorMsg = file.getOriginalFilename() + ": " + e.getMessage();
                errorMessages.add(errorMsg);
            }
        }

        model.addAttribute("uploadedFilenames", uploadedFilenames);
        model.addAttribute("errorMessages", errorMessages);
        model.addAttribute("totalCount", files.size());
        model.addAttribute("successCount", uploadedFilenames.size());

        return "community/images/upload-multiple-result";
    }

    /**
     * 이미지 조회 - 게시글용
     */
    @GetMapping("/post/{filename:.+}")
    public ResponseEntity<Resource> getPostImage(@PathVariable String filename) {
        return serveImage(filename);
    }

    /**
     * 이미지 썸네일 조회
     */
    @GetMapping("/thumb/{filename:.+}")
    public ResponseEntity<Resource> getThumbnail(@PathVariable String filename) {
        return serveThumbnail(filename);
    }

    /**
     * 이미지 파일 조회 (내부 메서드)
     */
    private ResponseEntity<Resource> serveImage(String filename) {
        try {
            Path imagePath = imageUploadService.getImagePath(filename);

            if (imagePath == null || !Files.exists(imagePath)) {
                log.warn("이미지를 찾을 수 없음: {}", filename);
                return ResponseEntity.notFound().build();
            }

            Resource resource = new UrlResource(imagePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                log.warn("이미지 읽기 실패: {}", filename);
                return ResponseEntity.notFound().build();
            }

            String contentType = getContentType(filename);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + filename + "\"")
                    .body(resource);

        } catch (MalformedURLException e) {
            log.error("이미지 URL 변환 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("이미지 조회 실패: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 썸네일 조회 (내부 메서드)
     */
    private ResponseEntity<Resource> serveThumbnail(String filename) {
        try {
            Path thumbnailPath = imageUploadService.getThumbnailPath(filename);

            if (thumbnailPath != null && Files.exists(thumbnailPath)) {
                Resource resource = new UrlResource(thumbnailPath.toUri());
                if (resource.exists() && resource.isReadable()) {
                    String contentType = getContentType(filename);
                    return ResponseEntity.ok()
                            .contentType(MediaType.parseMediaType(contentType))
                            .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                            .body(resource);
                }
            }

            // 썸네일이 없으면 원본 이미지 반환
            return serveImage(filename);

        } catch (Exception e) {
            log.error("썸네일 조회 실패: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 이미지 갤러리 페이지
     */
    @GetMapping("/gallery")
    public String imageGallery(@AuthenticationPrincipal UserSecurityDTO user,
                               Model model) {
        if (user == null || user.getUser() == null) {
            return "redirect:/login";
        }

        List<String> allImageFiles = imageUploadService.getAllImageFiles();
        model.addAttribute("images", allImageFiles);
        model.addAttribute("totalCount", allImageFiles.size());

        return "community/images/gallery";
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

        List<String> allImageFiles = imageUploadService.getAllImageFiles();
        model.addAttribute("images", allImageFiles);
        model.addAttribute("totalCount", allImageFiles.size());

        return "community/images/manage";
    }

    /**
     * 이미지 삭제 처리
     */
    @PostMapping("/delete")
    public String deleteImage(
            @AuthenticationPrincipal UserSecurityDTO user,
            @RequestParam String filename,
            RedirectAttributes redirectAttributes) {

        if (user == null || user.getUser() == null) {
            return "redirect:/login";
        }

        try {
            boolean deleted = imageUploadService.deleteImageByFilename(filename);
            if (deleted) {
                redirectAttributes.addFlashAttribute("message", "이미지가 삭제되었습니다.");
            } else {
                redirectAttributes.addFlashAttribute("error", "이미지를 찾을 수 없습니다.");
            }
        } catch (Exception e) {
            log.error("이미지 삭제 실패: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "이미지 삭제 실패: " + e.getMessage());
        }

        return "redirect:/community/images/manage";
    }

    /**
     * 업로드 성공 페이지
     */
    @GetMapping("/upload-success")
    public String uploadSuccess() {
        return "community/images/upload-success";
    }

    /**
     * 이미지 파일 유효성 검사
     */
    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어 있습니다.");
        }

        long maxSize = 60 * 1024;
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("이미지 크기는 60KB 이하여야 합니다. 현재: " +
                    (file.getSize() / 1024) + "KB");
        }

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

    /**
     * 파일 확장자로 MIME 타입 결정
     */
    private String getContentType(String filename) {
        String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();

        return switch (extension) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "bmp" -> "image/bmp";
            case "webp" -> "image/webp";
            default -> "application/octet-stream";
        };
    }
}