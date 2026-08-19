package com.soubhagya.api_rate_limiter.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

	@Bean
	public OpenAPI rateLimiterOpenAPI() {
		return new OpenAPI()
				.info(new Info()
						.title("API Rate Limiter")
						.description("Redis-powered API rate limiter built with Spring Boot and Docker.")
						.version("1.0.0"));
	}

}