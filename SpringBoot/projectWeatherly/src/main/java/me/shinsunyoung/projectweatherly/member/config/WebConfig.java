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

        // favicon 핸들러
        registry.addResourceHandler("/favicon.ico")
                .addResourceLocations("classpath:/static/favicon.ico")
                .setCachePeriod(3600);
    }
}