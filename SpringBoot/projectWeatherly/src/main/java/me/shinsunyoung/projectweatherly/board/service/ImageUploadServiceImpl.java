
package me.shinsunyoung.projectweatherly.board.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Service
@Slf4j
public class ImageUploadServiceImpl implements ImageUploadService {

    @Value("${image.upload.dir:uploads}")
    private String uploadDir;

    @Value("${image.max-file-size:5120000}") // 60KB in bytes
    private long maxFileSize;

    @Value("${image.allowed-extensions:jpg,jpeg,png,gif,bmp,webp}")
    private String allowedExtensions;

    @Value("${image.thumbnail.width:200}")
    private int thumbnailWidth;

    @Value("${image.thumbnail.height:200}")
    private int thumbnailHeight;

    @PostConstruct
    public void init() {
        try {
            // 업로드 디렉토리 생성
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                log.info("이미지 업로드 디렉토리 생성됨: {}", uploadPath.toAbsolutePath());
            }

            // 썸네일 디렉토리 생성
            Path thumbPath = Paths.get(uploadDir, "thumbs");
            if (!Files.exists(thumbPath)) {
                Files.createDirectories(thumbPath);
                log.info("썸네일 디렉토리 생성됨: {}", thumbPath.toAbsolutePath());
            }

        } catch (IOException e) {
            log.error("이미지 업로드 디렉토리 생성 실패: {}", e.getMessage());
            throw new RuntimeException("이미지 업로드 디렉토리 생성 실패", e);
        }
    }

    @Override
    public String uploadImage(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어 있습니다.");
        }

        // 파일 검증
        validateFileSize(file);
        validateFileExtension(file.getOriginalFilename());

        // 원본 파일명에서 확장자 추출
        String originalFilename = file.getOriginalFilename();
        String fileExtension = "";

        if (originalFilename != null && originalFilename.contains(".")) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        } else {
            fileExtension = ".jpg"; // 기본 확장자
        }

        // UUID로 파일명 생성
        String savedFilename = UUID.randomUUID().toString() + fileExtension;

        // 파일 저장
        Path filePath = Paths.get(uploadDir, savedFilename);
        Files.copy(file.getInputStream(), filePath);

        // 썸네일 생성
        createThumbnail(savedFilename);

        log.info("이미지 업로드 성공: {} -> {}", originalFilename, savedFilename);

        // 저장된 파일명 반환 (상대 경로 없이)
        return savedFilename;
    }

    @Override
    public List<String> uploadMultipleImages(List<MultipartFile> files) throws IOException {
        List<String> uploadedFilenames = new ArrayList<>();

        for (MultipartFile file : files) {
            if (file != null && !file.isEmpty()) {
                try {
                    String filename = uploadImage(file);
                    uploadedFilenames.add(filename);
                    log.debug("다중 이미지 업로드 성공: {}", file.getOriginalFilename());
                } catch (IOException e) {
                    log.error("이미지 업로드 실패: {}", file.getOriginalFilename(), e);
                    throw new IOException("파일 업로드 실패: " + file.getOriginalFilename(), e);
                }
            }
        }

        log.info("다중 이미지 업로드 완료: 총 {}개 중 {}개 성공", files.size(), uploadedFilenames.size());
        return uploadedFilenames;
    }

    @Override
    public Path getImagePath(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            return null;
        }

        // URL에서 파일명 추출 (URL인 경우)
        String cleanFilename = extractFilenameFromUrl(filename);

        return Paths.get(uploadDir, cleanFilename);
    }

    @Override
    public Path getThumbnailPath(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            return null;
        }

        // URL에서 파일명 추출 (URL인 경우)
        String cleanFilename = extractFilenameFromUrl(filename);

        return Paths.get(uploadDir, "thumbs", "thumb_" + cleanFilename);
    }

    @Override
    public boolean deleteImage(String imageUrl) throws IOException {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            return false;
        }

        String filename = extractFilenameFromUrl(imageUrl);
        return deleteImageByFilename(filename);
    }

    @Override
    public boolean deleteImageByFilename(String filename) throws IOException {
        if (filename == null || filename.trim().isEmpty()) {
            return false;
        }

        boolean deleted = false;

        // 원본 이미지 삭제
        Path imagePath = getImagePath(filename);
        if (imagePath != null && Files.exists(imagePath)) {
            Files.delete(imagePath);
            deleted = true;
            log.info("이미지 삭제: {}", filename);
        }

        // 썸네일 삭제
        Path thumbPath = getThumbnailPath(filename);
        if (thumbPath != null && Files.exists(thumbPath)) {
            Files.delete(thumbPath);
            log.info("썸네일 삭제: {}", thumbPath.getFileName());
        }

        return deleted;
    }

    @Override
    public List<String> getAllImageFiles() {
        List<String> imageFiles = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(Paths.get(uploadDir), 1)) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> {
                        String filename = path.getFileName().toString();
                        return !filename.startsWith("thumb_") &&
                                !path.toString().contains("thumbs") &&
                                isImageFile(filename);
                    })
                    .map(path -> path.getFileName().toString())
                    .forEach(imageFiles::add);

        } catch (IOException e) {
            log.error("이미지 파일 목록 조회 실패", e);
        }

        return imageFiles;
    }

    @Override
    public boolean imageExists(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            return false;
        }

        String cleanFilename = extractFilenameFromUrl(filename);
        Path imagePath = Paths.get(uploadDir, cleanFilename);
        return Files.exists(imagePath);
    }

    @Override
    public String extractFilenameFromUrl(String imageUrl) {
        if (imageUrl == null) {
            return null;
        }

        // URL 형식에서 파일명 추출
        if (imageUrl.contains("/")) {
            return imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
        }

        return imageUrl;
    }

    @Override
    public void validateFileSize(MultipartFile file) {
        if (file == null) {
            throw new IllegalArgumentException("파일이 null입니다.");
        }

        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException(
                    String.format("이미지 크기는 %dKB 이하여야 합니다. 현재: %dKB",
                            maxFileSize / 1024, file.getSize() / 1024)
            );
        }
    }

    @Override
    public void validateFileExtension(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            throw new IllegalArgumentException("파일 이름이 없습니다.");
        }

        String extension = getFileExtension(filename).toLowerCase();
        String[] allowed = allowedExtensions.split(",");

        boolean isValid = false;
        for (String ext : allowed) {
            if (ext.trim().equalsIgnoreCase(extension)) {
                isValid = true;
                break;
            }
        }

        if (!isValid) {
            throw new IllegalArgumentException(
                    String.format("지원하지 않는 파일 형식입니다. 허용된 형식: %s", allowedExtensions)
            );
        }
    }

    @Override
    public void createThumbnail(String filename) throws IOException {
        Path imagePath = getImagePath(filename);
        if (imagePath == null || !Files.exists(imagePath)) {
            log.warn("썸네일 생성 실패: 원본 이미지를 찾을 수 없음 - {}", filename);
            return;
        }

        try {
            BufferedImage originalImage = ImageIO.read(imagePath.toFile());
            if (originalImage == null) {
                log.warn("썸네일 생성 실패: 이미지를 읽을 수 없음 - {}", filename);
                return;
            }

            // 원본 이미지 크기
            int originalWidth = originalImage.getWidth();
            int originalHeight = originalImage.getHeight();

            // 썸네일 비율 계산
            double widthRatio = (double) thumbnailWidth / originalWidth;
            double heightRatio = (double) thumbnailHeight / originalHeight;
            double ratio = Math.min(widthRatio, heightRatio);

            // 새 크기 계산 (비율 유지)
            int thumbWidth = (int) (originalWidth * ratio);
            int thumbHeight = (int) (originalHeight * ratio);

            // 썸네일 생성
            BufferedImage thumbnail = new BufferedImage(thumbWidth, thumbHeight,
                    originalImage.getType() == 0 ? BufferedImage.TYPE_INT_RGB : originalImage.getType());

            Graphics2D g2d = thumbnail.createGraphics();

            // 이미지 품질 설정
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            // 이미지 그리기
            g2d.drawImage(originalImage, 0, 0, thumbWidth, thumbHeight, null);
            g2d.dispose();

            // 썸네일 저장
            String extension = getFileExtension(filename);
            Path thumbDir = Paths.get(uploadDir, "thumbs");
            Path thumbPath = thumbDir.resolve("thumb_" + filename);

            // 썸네일 디렉토리 확인
            if (!Files.exists(thumbDir)) {
                Files.createDirectories(thumbDir);
            }

            // 이미지 저장 (확장자에 따라 포맷 지정)
            String format = extension.toLowerCase();
            if (format.equals("jpg") || format.equals("jpeg")) {
                format = "JPEG";
            } else if (format.equals("webp")) {
                // Java 기본 라이브러리는 webp를 지원하지 않으므로 PNG로 변환
                format = "PNG";
                thumbPath = Paths.get(uploadDir, "thumbs", "thumb_" +
                        filename.substring(0, filename.lastIndexOf(".")) + ".png");
            }

            ImageIO.write(thumbnail, format, thumbPath.toFile());

            log.debug("썸네일 생성 완료: {} -> {}", filename, thumbPath.getFileName());

        } catch (IOException e) {
            log.error("썸네일 생성 실패: {}", filename, e);
            throw new IOException("썸네일 생성 실패: " + filename, e);
        } catch (Exception e) {
            log.error("썸네일 생성 중 오류 발생: {}", filename, e);
            throw new IOException("썸네일 생성 중 오류 발생", e);
        }
    }

    /**
     * 파일 확장자 추출
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }

    /**
     * 이미지 파일 여부 확인
     */
    private boolean isImageFile(String filename) {
        if (filename == null) {
            return false;
        }

        String extension = getFileExtension(filename).toLowerCase();
        String[] allowed = allowedExtensions.split(",");

        for (String ext : allowed) {
            if (ext.trim().equalsIgnoreCase(extension)) {
                return true;
            }
        }

        return false;
    }
}