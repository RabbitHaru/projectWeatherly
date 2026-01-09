package me.shinsunyoung.projectweatherly.common.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApiKeyManager {

    private final PropertiesManager propertiesManager;

    private Map<String, String> apiKeys = new HashMap<>();

    @PostConstruct
    public void init() {
        loadApiKeys();
        validateApiKeys();
    }

    private void loadApiKeys() {
        apiKeys.clear();

        // 기상청 API 키
        apiKeys.put("KMA", propertiesManager.getKmaApiKey());

        // 에어코리아 API 키
        apiKeys.put("AIR_KOREA", propertiesManager.getAirKoreaApiKey());

        // 카카오 API 키
        apiKeys.put("KAKAO", propertiesManager.getKakaoApiKey());

        // 네이버 API 키
        apiKeys.put("NAVER_CLIENT_ID", propertiesManager.getNaverClientId());
        apiKeys.put("NAVER_CLIENT_SECRET", propertiesManager.getNaverClientSecret());

        log.debug("API keys loaded successfully");
    }

    private void validateApiKeys() {
        int validCount = 0;
        int totalCount = apiKeys.size();

        for (Map.Entry<String, String> entry : apiKeys.entrySet()) {
            String keyName = entry.getKey();
            String keyValue = entry.getValue();

            boolean isValid = isValidApiKey(keyValue);

            if (isValid) {
                validCount++;
                log.debug("✅ API key '{}' is valid", keyName);
            } else {
                log.warn("⚠️ API key '{}' is invalid or not set", keyName);
            }
        }

        log.info("API Key Validation: {}/{} keys are valid", validCount, totalCount);

        if (validCount == 0) {
            log.error("❌ No valid API keys found. Service may not work properly.");
        }
    }

    private boolean isValidApiKey(String apiKey) {
        return apiKey != null &&
                !apiKey.trim().isEmpty() &&
                !apiKey.contains("YOUR_") &&
                !apiKey.contains("여기에") &&
                apiKey.length() > 10;
    }

    // 기상청 API 키
    public String getKmaApiKey() {
        return apiKeys.get("KMA");
    }

    public boolean hasKmaApiKey() {
        return isValidApiKey(getKmaApiKey());
    }

    // 에어코리아 API 키
    public String getAirKoreaApiKey() {
        return apiKeys.get("AIR_KOREA");
    }

    public boolean hasAirKoreaApiKey() {
        return isValidApiKey(getAirKoreaApiKey());
    }

    // 카카오 API 키
    public String getKakaoApiKey() {
        return apiKeys.get("KAKAO");
    }

    public boolean hasKakaoApiKey() {
        return isValidApiKey(getKakaoApiKey());
    }

    // 네이버 API 키
    public String getNaverClientId() {
        return apiKeys.get("NAVER_CLIENT_ID");
    }

    public String getNaverClientSecret() {
        return apiKeys.get("NAVER_CLIENT_SECRET");
    }

    public boolean hasNaverApiKey() {
        return isValidApiKey(getNaverClientId()) && isValidApiKey(getNaverClientSecret());
    }

    // 모든 API 키 상태 확인
    public Map<String, Boolean> getApiKeyStatus() {
        Map<String, Boolean> status = new HashMap<>();

        status.put("KMA", hasKmaApiKey());
        status.put("AIR_KOREA", hasAirKoreaApiKey());
        status.put("KAKAO", hasKakaoApiKey());
        status.put("NAVER", hasNaverApiKey());

        return status;
    }

    // API 키 리로드
    public void reload() {
        loadApiKeys();
        validateApiKeys();
        log.info("API keys reloaded");
    }

    // API 키 마스킹
    public String getMaskedKmaApiKey() {
        return propertiesManager.maskSensitiveInfo(getKmaApiKey());
    }

    public String getMaskedAirKoreaApiKey() {
        return propertiesManager.maskSensitiveInfo(getAirKoreaApiKey());
    }
}