package me.shinsunyoung.projectweatherly.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.Properties;

@Slf4j
public class ConfigLoader implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment,
                                       SpringApplication application) {

        // 설정 파일 로드 순서 (높은 우선순위가 나중에 로드됨)
        Resource[] configResources = {
                // 1. Classpath의 기본 설정 (가장 낮은 우선순위)
                new ClassPathResource("application.properties"),
                // 2. Classpath의 시크릿 설정
                new ClassPathResource("secret.properties"),
                // 3. 외부 config 디렉토리의 공개 설정
                new FileSystemResource("./config/application.properties"),
                // 4. 외부 config 디렉토리의 시크릿 설정 (가장 높은 우선순위)
                new FileSystemResource("./config/secret.properties")
        };

        for (Resource resource : configResources) {
            if (resource.exists()) {
                try {
                    Properties properties = new Properties();
                    properties.load(resource.getInputStream());

                    String sourceName = resource.getDescription();
                    environment.getPropertySources().addFirst(
                            new PropertiesPropertySource(sourceName, properties)
                    );

                    log.info("✅ Loaded configuration from: {}", resource.getURI());
                } catch (IOException e) {
                    log.warn("⚠️ Could not load configuration from: {}", resource.getFilename());
                }
            }
        }
    }
}