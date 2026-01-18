
package me.shinsunyoung.projectweatherly.util;

import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

public class ImageUtils {

    public static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
            "jpg", "jpeg", "png", "gif", "bmp", "webp"
    );

    public static final long MAX_FILE_SIZE = 60 * 1024; // 60KB

    /**
     * 이미지 파일인지 확인
     */
    public static boolean isImageFile(String filename) {
        if (filename == null) {
            return false;
        }

        String extension = getFileExtension(filename).toLowerCase();
        return ALLOWED_EXTENSIONS.contains(extension);
    }

    /**
     * 파일 확장자 추출
     */
    public static String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }

    /**
     * 파일 크기 검증
     */
    public static void validateFileSize(MultipartFile file) {
        if (file == null) {
            throw new IllegalArgumentException("파일이 null입니다.");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                    String.format("이미지 크기는 %dKB 이하여야 합니다. 현재: %dKB",
                            MAX_FILE_SIZE / 1024, file.getSize() / 1024)
            );
        }
    }

    /**
     * 파일 확장자 검증
     */
    public static void validateFileExtension(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            throw new IllegalArgumentException("파일 이름이 없습니다.");
        }

        String extension = getFileExtension(filename).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException(
                    String.format("지원하지 않는 파일 형식입니다. 허용된 형식: %s",
                            String.join(", ", ALLOWED_EXTENSIONS))
            );
        }
    }

    /**
     * URL에서 파일명 추출
     */
    public static String extractFilenameFromUrl(String url) {
        if (url == null) {
            return null;
        }

        if (url.contains("/")) {
            return url.substring(url.lastIndexOf("/") + 1);
        }

        return url;
    }
}