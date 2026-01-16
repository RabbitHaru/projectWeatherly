package me.shinsunyoung.projectweatherly.board.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Getter
@Setter
public class BoardRequest {
    private String title;
    private String content;
    private String category;
    private List<MultipartFile> imageFiles; // 다중 파일 업로드
}