package me.shinsunyoung.projectweatherly.s3.controller;

import lombok.RequiredArgsConstructor;
import me.shinsunyoung.projectweatherly.s3.service.S3Service;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/S3")
@RequiredArgsConstructor
public class S3Controller{
    private final S3Service s3Service;

    @PostMapping("/upload")
    public ResponseEntity<String> upload(@RequestParam("file") MultipartFile file){
        try{
            return ResponseEntity.ok(s3Service.uploadImg(file));
        } catch(Exception e){
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }
    @GetMapping("/download")
    public ResponseEntity<String> download(
            @RequestParam("fileName")String fileName){
        return ResponseEntity.ok(s3Service.downloadImg(fileName));
    }

    @GetMapping("/cloud")
    public ResponseEntity<String> cloud(
            @RequestParam("fileName") String fileName){
        return ResponseEntity.ok(s3Service.getCloudFrontUrl(fileName));
    }
}
