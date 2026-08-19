package com.soubhagya.api_rate_limiter.controller;

import com.soubhagya.api_rate_limiter.model.RateLimitResponse;
import com.soubhagya.api_rate_limiter.model.RateLimitStatusResponse;
import com.soubhagya.api_rate_limiter.service.RateLimiterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Rate Limiter", description = "Endpoints protected by the Redis-based rate limiter")
@RestController
@RequestMapping("/api")
public class RateLimiterController {

	private final RateLimiterService rateLimiterService;

	public RateLimiterController(RateLimiterService rateLimiterService) {
		this.rateLimiterService = rateLimiterService;
	}

	@Operation(summary = "Test endpoint protected by the rate limiter",
			description = "Each request is tracked per client IP. A fixed window allows up to 5 requests "
					+ "per 60 seconds; further requests are rejected with HTTP 429 until the window expires.")
	@ApiResponses({
			@ApiResponse(responseCode = "200",
					description = "Request allowed. remainingRequests shows how many more requests are left in the current 60 second window before the limit is reached.",
					content = @Content(schema = @Schema(implementation = RateLimitResponse.class),
							examples = @ExampleObject(name = "Allowed",
									value = "{\"success\": true, \"message\": \"Request allowed\", \"remainingRequests\": 4}"))),
			@ApiResponse(responseCode = "429",
					description = "Too many requests. The limit of 5 requests per 60 seconds has been reached. "
							+ "remainingRequests is 0 and retryAfterSeconds indicates the number of seconds to wait before the window resets.",
					headers = @Header(name = "Retry-After",
							description = "Number of seconds the client should wait before retrying",
							schema = @Schema(type = "integer")),
					content = @Content(schema = @Schema(implementation = RateLimitResponse.class),
							examples = @ExampleObject(name = "Rate limited",
									value = "{\"success\": false, \"message\": \"Too many requests\", \"remainingRequests\": 0, \"retryAfterSeconds\": 45}")))
	})
	@GetMapping("/test")
	public RateLimitResponse test(HttpServletRequest request) {
		String clientIp = request.getRemoteAddr();
		return rateLimiterService.consume(clientIp);
	}

	@Operation(summary = "Get the current rate-limit status for the requesting client IP",
			description = "Read-only endpoint that reports how many requests the calling client IP has already consumed "
					+ "in the current window and how many remain. It does NOT consume a request. "
					+ "status is READY when no window exists yet, ACTIVE while the window is in use, "
					+ "and LIMIT_REACHED when the limit has been consumed.")
	@ApiResponses({
			@ApiResponse(responseCode = "200",
					description = "Rate-limit status for the requesting client IP. "
							+ "remaining is max(limit - used, 0) and resetInSeconds is the Redis TTL of the client's "
							+ "rate-limit key (0 when no window exists yet).",
					content = @Content(schema = @Schema(implementation = RateLimitStatusResponse.class),
							examples = @ExampleObject(name = "Active window",
									value = "{\"limit\": 5, \"used\": 2, \"remaining\": 3, \"windowSeconds\": 60, "
											+ "\"resetInSeconds\": 42, \"status\": \"ACTIVE\"}")))
	})
	@GetMapping("/rate-limit/status")
	public RateLimitStatusResponse status(HttpServletRequest request) {
		String clientIp = request.getRemoteAddr();
		return rateLimiterService.getStatus(clientIp);
	}

}