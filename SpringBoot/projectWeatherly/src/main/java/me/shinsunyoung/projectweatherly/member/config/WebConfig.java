package me.shinsunyoung.projectweatherly.member.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 정적 리소스 핸들러
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");

        // 업로드된 이미지를 두 가지 경로로 서빙
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:./uploads/");

        // /images/ 경로도 uploads 디렉토리로 매핑
        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:./uploads/");

        // /default.png 직접 접근을 위한 매핑 추가
        registry.addResourceHandler("/default.png")
                .addResourceLocations("file:./uploads/default.png");
    }
}