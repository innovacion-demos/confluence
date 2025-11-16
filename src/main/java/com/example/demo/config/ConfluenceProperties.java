package com.example.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.confluence")
public record ConfluenceProperties(
    String baseUrl, String username, String apiToken, String spaceKey) {}
