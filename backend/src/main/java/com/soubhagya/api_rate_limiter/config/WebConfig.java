package com.soubhagya.api_rate_limiter.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

	private final RateLimitedInterceptor rateLimitedInterceptor;
	private final List<String> corsAllowedOrigins;

	public WebConfig(RateLimitedInterceptor rateLimitedInterceptor,
			@Value("${app.cors.allowed-origins}") String corsAllowedOrigins) {
		this.rateLimitedInterceptor = rateLimitedInterceptor;
		this.corsAllowedOrigins = Arrays.stream(corsAllowedOrigins.split(","))
				.map(String::trim)
				.filter(origin -> !origin.isEmpty())
				.toList();
	}

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/api/**")
				.allowedOrigins(corsAllowedOrigins.toArray(String[]::new))
				.allowedMethods("GET");
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(rateLimitedInterceptor).addPathPatterns("/api/**");
	}

}
