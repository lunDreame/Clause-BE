package com.clause.app.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${clause.cors.allowed-origins:*}")
    private String allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        var corsRegistration = registry.addMapping("/**")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("*")
                .maxAge(3600L);
        
        if ("*".equals(allowedOrigins)) {
            corsRegistration.allowedOriginPatterns("*")
                    .allowCredentials(false);
        } else {
            String[] origins = allowedOrigins.split(",");
            corsRegistration.allowedOrigins(origins)
                    .allowCredentials(true);
        }
    }
}

