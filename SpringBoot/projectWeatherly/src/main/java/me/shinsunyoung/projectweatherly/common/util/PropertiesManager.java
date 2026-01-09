package me.shinsunyoung.projectweatherly.common.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Slf4j
@Component
public class PropertiesManager {

    private final Properties secretProperties = new Properties();
    private final Properties publicProperties = new Properties();

    @PostConstruct
    public void init() {
        loadProperties();
    }

    private void loadProperties() {
        // 시크릿 프로퍼티 로드
        Resource[] secretResources = {
                new FileSystemResource("config/secret.properties"),
                new ClassPathResource("config/secret.properties"),
                new FileSystemResource("secret.properties"),
                new ClassPathResource("secret.properties")
        };

        for (Resource resource : secretResources) {
            if (resource.exists()) {
                try (InputStream inputStream = resource.getInputStream()) {
                    secretProperties.load(inputStream);
                    log.info("✅ Secret properties loaded from: {}", resource.getDescription());
                    break;
                } catch (IOException e) {
                    log.warn("⚠️ Failed to load secret properties from: {}", resource.getDescription());
                }
            }
        }

        // 공개 프로퍼티 로드
        Resource[] publicResources = {
                new FileSystemResource("config/application.properties"),
                new ClassPathResource("application.properties")
        };

        for (Resource resource : publicResources) {
            if (resource.exists()) {
                try (InputStream inputStream = resource.getInputStream()) {
                    publicProperties.load(inputStream);
                    log.info("✅ Public properties loaded from: {}", resource.getDescription());
                    break;
                } catch (IOException e) {
                    log.warn("⚠️ Failed to load public properties from: {}", resource.getDescription());
                }
            }
        }

        // 유효성 검사
        validateProperties();
    }

    private void validateProperties() {
        // API 키 검증
        String kmaKey = getProperty("api.kma.key", "");
        String airKoreaKey = getProperty("api.airkorea.key", "");

        if (kmaKey == null || kmaKey.trim().isEmpty() || kmaKey.contains("YOUR_") || kmaKey.contains("여기에")) {
            log.error("❌ 기상청(KMA) API 키가 설정되지 않았거나 기본값입니다. config/secret.properties 파일을 확인하세요.");
        }

        if (airKoreaKey == null || airKoreaKey.trim().isEmpty() || airKoreaKey.contains("YOUR_") || airKoreaKey.contains("여기에")) {
            log.error("❌ 에어코리아 API 키가 설정되지 않았거나 기본값입니다. config/secret.properties 파일을 확인하세요.");
        }
    }

    public String getProperty(String key, String defaultValue) {
        // 1. 시스템 환경변수 확인 (우선순위 가장 높음)
        String envKey = key.toUpperCase().replace('.', '_');
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.trim().isEmpty()) {
            log.debug("Using environment variable for {}: {}", key, maskSensitiveInfo(envValue));
            return envValue;
        }

        // 2. JVM 시스템 프로퍼티 확인
        String sysValue = System.getProperty(key);
        if (sysValue != null && !sysValue.trim().isEmpty()) {
            log.debug("Using system property for {}: {}", key, maskSensitiveInfo(sysValue));
            return sysValue;
        }

        // 3. 시크릿 프로퍼티 확인
        String secretValue = secretProperties.getProperty(key);
        if (secretValue != null && !secretValue.trim().isEmpty()) {
            log.debug("Using secret property for {}: {}", key, maskSensitiveInfo(secretValue));
            return secretValue;
        }

        // 4. 공개 프로퍼티 확인
        String publicValue = publicProperties.getProperty(key);
        if (publicValue != null && !publicValue.trim().isEmpty()) {
            return publicValue;
        }

        // 5. 기본값 반환
        return defaultValue;
    }

    public String getProperty(String key) {
        return getProperty(key, null);
    }

    // API 키 관련 메서드
    public String getKmaApiKey() {
        return getProperty("api.kma.key", "");
    }

    public String getAirKoreaApiKey() {
        return getProperty("api.airkorea.key", "");
    }

    public String getKakaoApiKey() {
        return getProperty("api.kakao.key", "");
    }

    public String getNaverClientId() {
        return getProperty("api.naver.client-id", "");
    }

    public String getNaverClientSecret() {
        return getProperty("api.naver.client-secret", "");
    }

    // 데이터베이스 관련 메서드
    public String getDbPassword() {
        return getProperty("spring.datasource.password", "");
    }

    public String getDbUsername() {
        return getProperty("spring.datasource.username", "root");
    }

    public String getDbUrl() {
        return getProperty("spring.datasource.url", "jdbc:mysql://localhost:3306/weatherly");
    }

    // Redis 관련 메서드
    public String getRedisPassword() {
        return getProperty("spring.redis.password", "");
    }

    public String getRedisHost() {
        return getProperty("spring.redis.host", "localhost");
    }

    public int getRedisPort() {
        return Integer.parseInt(getProperty("spring.redis.port", "6379"));
    }

    // 서버 설정
    public int getServerPort() {
        return Integer.parseInt(getProperty("server.port", "8080"));
    }

    public String getServerContextPath() {
        return getProperty("server.servlet.context-path", "/");
    }

    // 로깅 레벨
    public String getLogLevel() {
        return getProperty("logging.level.com.weatherly", "INFO");
    }

    // 민감 정보 마스킹
    public String maskSensitiveInfo(String info) {
        if (info == null || info.length() < 8) {
            return "***";
        }
        return info.substring(0, 4) + "***" + info.substring(info.length() - 4);
    }

    // 모든 프로퍼티 가져오기 (디버깅용)
    public Properties getAllProperties() {
        Properties allProps = new Properties();
        allProps.putAll(publicProperties);
        allProps.putAll(secretProperties);
        return allProps;
    }

    // 프로퍼티 다시 로드
    public void reload() {
        loadProperties();
        log.info("Properties reloaded");
    }
}