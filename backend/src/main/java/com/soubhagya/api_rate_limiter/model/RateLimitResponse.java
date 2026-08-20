package com.soubhagya.api_rate_limiter.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RateLimitResponse(
		@Schema(description = "Whether the request was allowed", example = "true") boolean success,
		@Schema(description = "Human readable status message", example = "Request allowed") String message,
		@Schema(description = "Requests still available in the current window. 0 when the rate limit has been reached.",
				example = "4") long remainingRequests,
		@Schema(description = "Seconds to wait before retrying. Only present in the HTTP 429 response.",
				example = "45") Long retryAfterSeconds,
		@JsonIgnore int limit,
		@JsonIgnore long resetInSeconds) {

	public static RateLimitResponse allowed(long remainingRequests) {
		return new RateLimitResponse(true, "Request allowed", remainingRequests, null, 0, 0L);
	}

	public static RateLimitResponse allowed(long remainingRequests, int limit, long resetInSeconds) {
		return new RateLimitResponse(true, "Request allowed", remainingRequests, null, limit, resetInSeconds);
	}

	public static RateLimitResponse blocked(long retryAfterSeconds) {
		return new RateLimitResponse(false, "Too many requests", 0, retryAfterSeconds, 0, 0L);
	}

	public static RateLimitResponse blocked(long retryAfterSeconds, int limit, long resetInSeconds) {
		return new RateLimitResponse(false, "Too many requests", 0, retryAfterSeconds, limit, resetInSeconds);
	}

}