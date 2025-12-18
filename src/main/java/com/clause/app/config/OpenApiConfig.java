package com.clause.app.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI clauseOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Clause API")
                        .description("계약서 분석 서비스 API")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Clause Team")
                                .email("support@clause.example.com")));
    }
}

