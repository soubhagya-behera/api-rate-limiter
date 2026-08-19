package com.soubhagya.api_rate_limiter.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RateLimitResponse(boolean success, String message, long remainingRequests, Long retryAfterSeconds) {

	public static RateLimitResponse allowed(long remainingRequests) {
		return new RateLimitResponse(true, "Request allowed", remainingRequests, null);
	}

	public static RateLimitResponse blocked(long retryAfterSeconds) {
		return new RateLimitResponse(false, "Too many requests", 0, retryAfterSeconds);
	}

}