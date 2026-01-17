
package me.shinsunyoung.projectweatherly.board.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface ImageUploadService {

    /**
     * 단일 이미지 업로드
     */
    String uploadImage(MultipartFile file) throws IOException;

    /**
     * 다중 이미지 업로드
     */
    List<String> uploadMultipleImages(List<MultipartFile> files) throws IOException;

    /**
     * 이미지 파일 경로 가져오기
     */
    Path getImagePath(String filename);

    /**
     * 썸네일 경로 가져오기
     */
    Path getThumbnailPath(String filename);

    /**
     * 이미지 삭제
     */
    boolean deleteImage(String imageUrl) throws IOException;

    /**
     * 이미지 삭제 (파일명으로)
     */
    boolean deleteImageByFilename(String filename) throws IOException;

    /**
     * 모든 이미지 파일 목록 가져오기
     */
    List<String> getAllImageFiles();

    /**
     * 이미지 파일 존재 여부 확인
     */
    boolean imageExists(String filename);

    /**
     * 이미지 URL을 파일명으로 변환
     */
    String extractFilenameFromUrl(String imageUrl);

    /**
     * 파일 크기 검증
     */
    void validateFileSize(MultipartFile file);

    /**
     * 파일 확장자 검증
     */
    void validateFileExtension(String filename);

    /**
     * 썸네일 생성
     */
    void createThumbnail(String filename) throws IOException;
}