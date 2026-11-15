package com.example.extracttool.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableConfigurationProperties(FacebookProperties.class)
@EnableScheduling
public class AppConfiguration {
}
