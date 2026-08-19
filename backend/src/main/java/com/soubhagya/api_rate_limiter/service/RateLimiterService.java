package com.soubhagya.api_rate_limiter.service;

import com.soubhagya.api_rate_limiter.config.RateLimiterProperties;
import com.soubhagya.api_rate_limiter.exception.RateLimitExceededException;
import com.soubhagya.api_rate_limiter.model.RateLimitResponse;
import com.soubhagya.api_rate_limiter.model.RateLimitStatusResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
public class RateLimiterService {

	private static final String KEY_PATTERN = "%s:%s";

	private final StringRedisTemplate redisTemplate;
	private final RateLimiterProperties properties;

	public RateLimiterService(StringRedisTemplate redisTemplate, RateLimiterProperties properties) {
		this.redisTemplate = redisTemplate;
		this.properties = properties;
	}

	public RateLimitResponse consume(String clientIp) {
		String key = String.format(KEY_PATTERN, properties.getKeyPrefix(), clientIp);

		Long count = redisTemplate.opsForValue().increment(key);

		if (count != null && count == 1L) {
			redisTemplate.expire(key, Duration.ofSeconds(properties.getWindowSeconds()));
		}

		if (count == null || count > properties.getMaxRequests()) {
			if (count != null) {
				redisTemplate.opsForValue().decrement(key);
			}
			long retryAfterSeconds = redisTemplate.getExpire(key, TimeUnit.SECONDS);
			throw new RateLimitExceededException(retryAfterSeconds);
		}

		long remainingRequests = (long) properties.getMaxRequests() - count;
		return RateLimitResponse.allowed(remainingRequests);
	}

	public RateLimitStatusResponse getStatus(String clientIp) {
		String key = String.format(KEY_PATTERN, properties.getKeyPrefix(), clientIp);
		int limit = properties.getMaxRequests();
		int windowSeconds = properties.getWindowSeconds();

		String value = redisTemplate.opsForValue().get(key);

		long used = 0;
		long resetInSeconds = 0;
		String status = "READY";

		if (value != null) {
			used = Long.parseLong(value);
			Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
			resetInSeconds = ttl != null ? Math.max(ttl, 0L) : 0L;
			status = used >= limit ? "LIMIT_REACHED" : "ACTIVE";
		}

		long remaining = Math.max((long) limit - used, 0L);
		return new RateLimitStatusResponse(limit, used, remaining, windowSeconds, resetInSeconds, status);
	}

}