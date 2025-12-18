package com.clause.app;

import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
public class ClauseApplication {

    public static void main(String[] args) {
        // .env 파일 로드
        try {
            Dotenv dotenv = Dotenv.configure()
                    .ignoreIfMissing() // .env 파일이 없어도 에러 발생하지 않음
                    .load();
            
            // .env 파일의 변수를 시스템 프로퍼티로 설정
            dotenv.entries().forEach(entry -> {
                String key = entry.getKey();
                String value = entry.getValue();
                if (System.getProperty(key) == null) {
                    System.setProperty(key, value);
                    log.debug("Loaded from .env: {}={}", key, maskSensitiveValue(key, value));
                }
            });
            
            log.info(".env file loaded successfully");
        } catch (Exception e) {
            log.warn("Failed to load .env file: {}", e.getMessage());
        }
        
        SpringApplication.run(ClauseApplication.class, args);
    }
    
    private static String maskSensitiveValue(String key, String value) {
        if (key != null && (key.contains("KEY") || key.contains("SECRET") || key.contains("PASSWORD") || key.contains("TOKEN"))) {
            return value != null && value.length() > 4 ? value.substring(0, 4) + "***" : "***";
        }
        return value;
    }
}
