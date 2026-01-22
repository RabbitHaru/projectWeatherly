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

    // 사용자가 업로드한 파일들
    private List<MultipartFile> imageFiles;

    // [★추가] DB에 저장할 파일 이름들
    private List<String> imageUrls;
}