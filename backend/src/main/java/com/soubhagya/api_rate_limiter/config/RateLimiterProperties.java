package com.soubhagya.api_rate_limiter.config;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Validated
@ConfigurationProperties(prefix = "rate-limit")
public class RateLimiterProperties {

	@Positive
	private int maxRequests = 5;

	@Positive
	private int windowSeconds = 60;

	private String keyPrefix = "rate-limit:ip";

	public int getMaxRequests() {
		return maxRequests;
	}

	public void setMaxRequests(int maxRequests) {
		this.maxRequests = maxRequests;
	}

	public int getWindowSeconds() {
		return windowSeconds;
	}

	public void setWindowSeconds(int windowSeconds) {
		this.windowSeconds = windowSeconds;
	}

	public String getKeyPrefix() {
		return keyPrefix;
	}

	public void setKeyPrefix(String keyPrefix) {
		this.keyPrefix = keyPrefix;
	}

}