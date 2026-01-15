package me.shinsunyoung.projectweatherly.member.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Component
public class FileUtil {
    @Value("${weatherly.upload.path}")
    private String uploadDir;

    public List<FileNameUtil> uploadFile(List<MultipartFile> files){
        List<FileNameUtil> nameList = new ArrayList<>();
        // 파일이 있는지 확인
        if(files!=null){
            for(MultipartFile file : files){
                if(file.getOriginalFilename().equals("")){
                    continue;
                }
                //원본 파일 이름
                String originalName = file.getOriginalFilename();
                // UUID : 범용 고유 식별자
                String uuid = UUID.randomUUID().toString();
                // 파일의 경로 및 이름 설정
                Path path = Paths.get(uploadDir,uuid+"_"+originalName);
                try{
                    file.transferTo(path); // 파일 저장
                    //원본, 새로운 파일 이름 저장
                    nameList.add(FileNameUtil.builder()
                            .originalFileName(originalName)
                            .newFileName(uuid+"_"+originalName)
                            .build());
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
        }
        return nameList;
    }

    public boolean fileRemove(String fileName){
        // upload폴더에 있는 파일이름을 찾아서 파일을 저장
        Resource resource = new FileSystemResource(
                uploadDir+ File.separator+ fileName);
        String resourceName = resource.getFilename();
        // 삭제 결과를 저장할 Map
        Map<String,Boolean> resultMap = new HashMap<>();
        boolean removed = false;
        try{
            String contentType = Files.probeContentType((resource.getFile().toPath()));
            if(contentType.startsWith("image/")){
                // 썸네일 삭제 코드
            }
            // 원본 파일 삭제 후 결과 저장
            removed = resource.getFile().delete();
        }catch(Exception e){
            e.printStackTrace();
        }
        // 정상 처리시 삭제 결과를 반환
        return removed;
    }
}
