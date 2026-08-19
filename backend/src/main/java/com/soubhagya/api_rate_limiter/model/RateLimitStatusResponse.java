package com.soubhagya.api_rate_limiter.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Current rate-limit status for a client IP")
public record RateLimitStatusResponse(
		@Schema(description = "Maximum number of requests allowed per window", example = "5") int limit,
		@Schema(description = "Number of requests already consumed in the current window", example = "2") long used,
		@Schema(description = "Requests still available in the current window. Never negative.", example = "3") long remaining,
		@Schema(description = "Length of the rate-limit window in seconds", example = "60") int windowSeconds,
		@Schema(description = "Seconds until the current window resets. 0 when no window is active.", example = "42") long resetInSeconds,
		@Schema(description = "Current status of the window: ACTIVE when a window is in use, READY when no window exists yet, LIMIT_REACHED when the limit has been consumed.",
				allowableValues = { "ACTIVE", "READY", "LIMIT_REACHED" }, example = "ACTIVE") String status) {
}