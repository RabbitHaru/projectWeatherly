package me.shinsunyoung.projectweatherly.board.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
public class BoardUpdateRequest {
    private String title;
    private String content;
    private String category;
    private List<String> deleteImages;  // 삭제할 이미지 URL 목록
    private List<MultipartFile> newImages;  // 새로 추가할 이미지
}