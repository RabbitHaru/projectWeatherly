package me.shinsunyoung.projectweatherly.board.controller;

import lombok.RequiredArgsConstructor;
import me.shinsunyoung.projectweatherly.board.service.ImageUploadService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/community/images")
@RequiredArgsConstructor
public class ImageUploadController {

    private final ImageUploadService imageUploadService;

    // 이미지 업로드 폼 페이지
    @GetMapping("/upload")
    public String uploadForm(@SessionAttribute(name = "memberId", required = false) Long memberId,
                             Model model) {
        if (memberId == null) {
            return "redirect:/login";
        }
        return "community/images/upload";
    }

    // 단일 이미지 업로드 처리
    @PostMapping("/upload")
    public String uploadImage(
            @SessionAttribute("memberId") Long memberId,
            @RequestParam("file") MultipartFile file,
            RedirectAttributes redirectAttributes,
            Model model) {

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

        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "community/images/upload";
        } catch (IOException e) {
            model.addAttribute("error", "이미지 업로드 실패: " + e.getMessage());
            return "community/images/upload";
        }

        return "community/images/upload-success";
    }

    // 다중 이미지 업로드 처리
    @PostMapping("/upload-multiple")
    public String uploadMultipleImages(
            @SessionAttribute("memberId") Long memberId,
            @RequestParam("files") List<MultipartFile> files,
            RedirectAttributes redirectAttributes,
            Model model) {

        List<String> uploadedUrls = new ArrayList<>();
        List<String> errorMessages = new ArrayList<>();

        for (MultipartFile file : files) {
            try {
                validateImageFile(file);
                String imageUrl = imageUploadService.uploadImage(file);
                uploadedUrls.add(imageUrl);
            } catch (Exception e) {
                errorMessages.add(file.getOriginalFilename() + ": " + e.getMessage());
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

    // 이미지 관리 페이지
    @GetMapping("/manage")
    public String manageImages(@SessionAttribute(name = "memberId", required = false) Long memberId,
                               Model model) {
        if (memberId == null) {
            return "redirect:/login";
        }
        // TODO: 사용자가 업로드한 이미지 목록 조회 구현 필요
        return "community/images/manage";
    }

    // 이미지 삭제 처리
    @PostMapping("/delete")
    public String deleteImage(
            @SessionAttribute("memberId") Long memberId,
            @RequestParam String imageUrl,
            RedirectAttributes redirectAttributes) {

        try {
            // TODO: 이미지 삭제 서비스 구현 필요
            // imageUploadService.deleteImage(memberId, imageUrl);
            redirectAttributes.addFlashAttribute("message", "이미지가 삭제되었습니다.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "이미지 삭제 실패: " + e.getMessage());
        }

        return "redirect:/community/images/manage";
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