package com.soubhagya.api_rate_limiter.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

	private final RateLimitedInterceptor rateLimitedInterceptor;

	public WebConfig(RateLimitedInterceptor rateLimitedInterceptor) {
		this.rateLimitedInterceptor = rateLimitedInterceptor;
	}

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/api/**")
				.allowedOrigins("http://localhost:5173")
				.allowedMethods("GET");
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(rateLimitedInterceptor).addPathPatterns("/api/**");
	}

}