package me.shinsunyoung.projectweatherly.util;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileNameUtil {

    // 원본 파일명
    private String originalFileName;

    // [★복구] 서버에 저장된 파일명 (FileUtil이 찾는 이름이 바로 이것!)
    private String newFileName;
}