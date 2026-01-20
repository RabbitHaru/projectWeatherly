package me.shinsunyoung.projectweatherly.util;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FileNameUtil {
    private String originalFileName;
    private String newFileName;
}
