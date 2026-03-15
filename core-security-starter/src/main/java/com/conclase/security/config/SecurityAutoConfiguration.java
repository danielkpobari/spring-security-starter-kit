package com.conclase.security.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;

@AutoConfiguration
@EnableConfigurationProperties(SecurityProperties.class)
@ComponentScan(basePackages = "com.conclase.security")
public class SecurityAutoConfiguration {
}
