package me.shinsunyoung.projectweatherly.s3.service;

import io.awspring.cloud.s3.ObjectMetadata;
import io.awspring.cloud.s3.S3Resource;
import io.awspring.cloud.s3.S3Template;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3Service {
    private final S3Template s3Template;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucket;
    @Value("${spring.cloud.aws.cloudfront.url}")
    private String cloudFrontUrl;
    //S3 버킷에 파일 올리기 메서드
    public String uploadImg(MultipartFile file) throws IOException {
        String fileName = "upload/"+UUID.randomUUID()+"_"+file.getOriginalFilename();
        // S3Resource : S3 안에 있는 파일 객체
        // S3Template : 파일을 업로드 하거나 다운로드할때 사용하는 객체
        S3Resource s3Resource = s3Template.upload(
                bucket // 저장할 버킷 이름
                ,fileName // 파일 이름
                ,file.getInputStream(), // 실제 파일 스트림
                // 메타 데이터
                ObjectMetadata.builder().contentType(file.getContentType()).build()
        );
        return s3Resource.getURL().toString();
    }

    // 1. 퍼블릭 엑세스를 허용하고 직접 받는 방식(사용안함)
    // 2. 임시 URL(시간제한)을 만들어 사용하는 방식
    // 3. CloudFront를 사용하여 접속하는 방식

    // Presigned URL 방식
    public String downloadImg(String fileName){
        return s3Template.createSignedGetURL(
                bucket, // 버킷 이름
                fileName, // 파일 이름
                Duration.ofMinutes(1) // url 유지시간
        ).toString();
    }

    // CloudFront를 사용하여 접속하는 방식
    public String getCloudFrontUrl(String fileName){
        return cloudFrontUrl+"/"+fileName;
    }
}
