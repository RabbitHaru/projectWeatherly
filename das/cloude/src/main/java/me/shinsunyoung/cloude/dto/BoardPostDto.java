package me.shinsunyoung.cloude.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BoardPostDto {
    private Long id;
    private String title;
    private String username;
}
