package com.soubhagya.api_rate_limiter.controller;

import com.soubhagya.api_rate_limiter.model.RateLimitResponse;
import com.soubhagya.api_rate_limiter.service.RateLimiterService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class RateLimiterController {

	private final RateLimiterService rateLimiterService;

	public RateLimiterController(RateLimiterService rateLimiterService) {
		this.rateLimiterService = rateLimiterService;
	}

	@GetMapping("/test")
	public RateLimitResponse test(HttpServletRequest request) {
		String clientIp = request.getRemoteAddr();
		return rateLimiterService.consume(clientIp);
	}

}