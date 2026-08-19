package com.soubhagya.api_rate_limiter.service;

import com.soubhagya.api_rate_limiter.config.RateLimiterProperties;
import com.soubhagya.api_rate_limiter.exception.RateLimitExceededException;
import com.soubhagya.api_rate_limiter.model.RateLimitResponse;
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
			long retryAfterSeconds = redisTemplate.getExpire(key, TimeUnit.SECONDS);
			throw new RateLimitExceededException(retryAfterSeconds);
		}

		long remainingRequests = (long) properties.getMaxRequests() - count;
		return RateLimitResponse.allowed(remainingRequests);
	}

}