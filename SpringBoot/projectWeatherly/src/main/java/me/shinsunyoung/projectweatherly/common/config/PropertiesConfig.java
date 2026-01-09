package me.shinsunyoung.projectweatherly.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;

@Slf4j
@Configuration
@PropertySources({
        @PropertySource(value = "classpath:application.properties", ignoreResourceNotFound = true),
        @PropertySource(value = "file:./config/application.properties", ignoreResourceNotFound = true),
        @PropertySource(value = "file:./config/secret.properties", ignoreResourceNotFound = true),
        @PropertySource(value = "classpath:config/secret.properties", ignoreResourceNotFound = true),
        @PropertySource(value = "classpath:secret.properties", ignoreResourceNotFound = true)
})
public class PropertiesConfig {

    public PropertiesConfig() {
        log.info("Properties configuration initialized");
    }
}