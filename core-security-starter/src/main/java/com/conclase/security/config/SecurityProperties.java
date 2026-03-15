package com.conclase.security.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "security.jwt")
public class SecurityProperties {
    private String secret = "defaultSecretKeyThatShouldBeChangedInProductionEnvironment1234567890";
    private long expiration = 86400000; // 24 hours in milliseconds
    private String header = "Authorization";
    private String prefix = "Bearer ";
}
