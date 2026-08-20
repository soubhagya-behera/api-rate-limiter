package com.soubhagya.api_rate_limiter.controller;

import com.soubhagya.api_rate_limiter.config.RateLimitedInterceptor;
import com.soubhagya.api_rate_limiter.config.WebConfig;
import com.soubhagya.api_rate_limiter.exception.RateLimitExceededException;
import com.soubhagya.api_rate_limiter.model.RateLimitResponse;
import com.soubhagya.api_rate_limiter.model.RateLimitStatusResponse;
import com.soubhagya.api_rate_limiter.service.RateLimiterService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RateLimiterController.class)
@Import({WebConfig.class, RateLimitedInterceptor.class})
class RateLimitedControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private RateLimiterService rateLimiterService;

	private static MockHttpServletRequestBuilder requestWithClientIp(String path, String clientIp) {
		return get(path).with(request -> {
			request.setRemoteAddr(clientIp);
			return request;
		});
	}

	@Test
	void annotatedEndpointIsRateLimitedWithAnnotationConfiguration() throws Exception {
		when(rateLimiterService.consume("10.0.0.5", 10, 60))
				.thenReturn(RateLimitResponse.allowed(9));

		mockMvc.perform(requestWithClientIp("/api/demo", "10.0.0.5"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.message").value("Request allowed"))
				.andExpect(jsonPath("$.remainingRequests").value(9));

		verify(rateLimiterService).consume("10.0.0.5", 10, 60);
	}

	@Test
	void annotatedEndpointWithoutConfigurationDefersToGlobalSettings() throws Exception {
		when(rateLimiterService.consume("10.0.0.6", -1, -1))
				.thenReturn(RateLimitResponse.allowed(4));

		mockMvc.perform(requestWithClientIp("/api/test", "10.0.0.6"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.remainingRequests").value(4));

		verify(rateLimiterService).consume("10.0.0.6", -1, -1);
	}

	@Test
	void unannotatedEndpointIsNotRateLimited() throws Exception {
		when(rateLimiterService.getStatus("10.0.0.7"))
				.thenReturn(new RateLimitStatusResponse(5, 2, 3, 60, 40, "ACTIVE"));

		mockMvc.perform(requestWithClientIp("/api/rate-limit/status", "10.0.0.7"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.used").value(2));

		verify(rateLimiterService, never()).consume(anyString());
	}

	@Test
	void returns429WithRetryAfterWhenLimitReached() throws Exception {
		when(rateLimiterService.consume("10.0.0.8", 10, 60))
				.thenThrow(new RateLimitExceededException(37));

		mockMvc.perform(requestWithClientIp("/api/demo", "10.0.0.8"))
				.andExpect(status().isTooManyRequests())
				.andExpect(header().string("Retry-After", "37"))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("Too many requests"))
				.andExpect(jsonPath("$.remainingRequests").value(0))
				.andExpect(jsonPath("$.retryAfterSeconds").value(37));
	}

	@Test
	void statusEndpointRemainsReadOnly() throws Exception {
		when(rateLimiterService.getStatus("10.0.0.9"))
				.thenReturn(new RateLimitStatusResponse(5, 5, 0, 60, 20, "LIMIT_REACHED"));

		mockMvc.perform(requestWithClientIp("/api/rate-limit/status", "10.0.0.9"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("LIMIT_REACHED"));

		verify(rateLimiterService, never()).consume(anyString());
	}

	@Test
	void allowedResponseIncludesRateLimitHeaders() throws Exception {
		when(rateLimiterService.consume("10.0.0.10", -1, -1))
				.thenReturn(RateLimitResponse.allowed(4, 5, 57));

		mockMvc.perform(requestWithClientIp("/api/test", "10.0.0.10"))
				.andExpect(status().isOk())
				.andExpect(header().string("X-RateLimit-Limit", "5"))
				.andExpect(header().string("X-RateLimit-Remaining", "4"))
				.andExpect(header().string("X-RateLimit-Reset", "57"));
	}

	@Test
	void allowedResponseKeepsJsonBodyUnchanged() throws Exception {
		when(rateLimiterService.consume("10.0.0.11", -1, -1))
				.thenReturn(RateLimitResponse.allowed(4, 5, 57));

		mockMvc.perform(requestWithClientIp("/api/test", "10.0.0.11"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.message").value("Request allowed"))
				.andExpect(jsonPath("$.remainingRequests").value(4))
				.andExpect(jsonPath("$.retryAfterSeconds").doesNotExist())
				.andExpect(jsonPath("$.limit").doesNotExist())
				.andExpect(jsonPath("$.resetInSeconds").doesNotExist());
	}

	@Test
	void endpointSpecificRateLimitedHeadersUseAnnotationConfiguration() throws Exception {
		when(rateLimiterService.consume("10.0.0.12", 10, 60))
				.thenReturn(RateLimitResponse.allowed(8, 10, 45));

		mockMvc.perform(requestWithClientIp("/api/demo", "10.0.0.12"))
				.andExpect(status().isOk())
				.andExpect(header().string("X-RateLimit-Limit", "10"))
				.andExpect(header().string("X-RateLimit-Remaining", "8"))
				.andExpect(header().string("X-RateLimit-Reset", "45"));

		verify(rateLimiterService).consume("10.0.0.12", 10, 60);
	}

	@Test
	void rateLimitExceededIncludesRateLimitHeaders() throws Exception {
		when(rateLimiterService.consume("10.0.0.13", 10, 60))
				.thenThrow(new RateLimitExceededException(37, 10, 37));

		mockMvc.perform(requestWithClientIp("/api/demo", "10.0.0.13"))
				.andExpect(status().isTooManyRequests())
				.andExpect(header().string("Retry-After", "37"))
				.andExpect(header().string("X-RateLimit-Limit", "10"))
				.andExpect(header().string("X-RateLimit-Remaining", "0"))
				.andExpect(header().string("X-RateLimit-Reset", "37"));
	}

	@Test
	void rateLimitExceededKeepsJsonBodyUnchanged() throws Exception {
		when(rateLimiterService.consume("10.0.0.14", -1, -1))
				.thenThrow(new RateLimitExceededException(37, 5, 37));

		mockMvc.perform(requestWithClientIp("/api/test", "10.0.0.14"))
				.andExpect(status().isTooManyRequests())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("Too many requests"))
				.andExpect(jsonPath("$.remainingRequests").value(0))
				.andExpect(jsonPath("$.retryAfterSeconds").value(37))
				.andExpect(jsonPath("$.limit").doesNotExist())
				.andExpect(jsonPath("$.resetInSeconds").doesNotExist());
	}

	@Test
	void statusEndpointDoesNotEmitRateLimitHeaders() throws Exception {
		when(rateLimiterService.getStatus("10.0.0.15"))
				.thenReturn(new RateLimitStatusResponse(5, 2, 3, 60, 40, "ACTIVE"));

		mockMvc.perform(requestWithClientIp("/api/rate-limit/status", "10.0.0.15"))
				.andExpect(status().isOk())
				.andExpect(header().doesNotExist("X-RateLimit-Limit"))
				.andExpect(header().doesNotExist("X-RateLimit-Remaining"))
				.andExpect(header().doesNotExist("X-RateLimit-Reset"));
	}

}