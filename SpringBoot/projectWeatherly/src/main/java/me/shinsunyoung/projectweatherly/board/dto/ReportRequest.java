package me.shinsunyoung.projectweatherly.board.dto;

import lombok.Data;

@Data
public class ReportRequest {
    private String type; // "post" or "comment"
    private Long targetId;
    private String reason;
    private String details;
    private Long boardId;
}