package com.soubhagya.api_rate_limiter.config;

import com.soubhagya.api_rate_limiter.model.RateLimitResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;

public final class RateLimitHeaders {

	public static final String LIMIT = "X-RateLimit-Limit";
	public static final String REMAINING = "X-RateLimit-Remaining";
	public static final String RESET = "X-RateLimit-Reset";

	private RateLimitHeaders() {
	}

	public static void apply(HttpServletResponse response, RateLimitResponse rateLimitResponse) {
		response.setHeader(LIMIT, String.valueOf(rateLimitResponse.limit()));
		response.setHeader(REMAINING, String.valueOf(rateLimitResponse.remainingRequests()));
		response.setHeader(RESET, String.valueOf(rateLimitResponse.resetInSeconds()));
	}

	public static void apply(HttpHeaders headers, RateLimitResponse rateLimitResponse) {
		headers.set(LIMIT, String.valueOf(rateLimitResponse.limit()));
		headers.set(REMAINING, String.valueOf(rateLimitResponse.remainingRequests()));
		headers.set(RESET, String.valueOf(rateLimitResponse.resetInSeconds()));
	}

}