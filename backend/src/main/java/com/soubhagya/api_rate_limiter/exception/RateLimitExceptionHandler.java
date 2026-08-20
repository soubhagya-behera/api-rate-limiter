package com.soubhagya.api_rate_limiter.exception;

import com.soubhagya.api_rate_limiter.config.RateLimitHeaders;
import com.soubhagya.api_rate_limiter.model.RateLimitResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class RateLimitExceptionHandler {

	@ExceptionHandler(RateLimitExceededException.class)
	public ResponseEntity<RateLimitResponse> handleRateLimitExceeded(RateLimitExceededException ex) {
		RateLimitResponse body = RateLimitResponse.blocked(ex.getRetryAfterSeconds(), ex.getLimit(), ex.getResetInSeconds());
		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.RETRY_AFTER, String.valueOf(ex.getRetryAfterSeconds()));
		RateLimitHeaders.apply(headers, body);
		return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).headers(headers).body(body);
	}

}