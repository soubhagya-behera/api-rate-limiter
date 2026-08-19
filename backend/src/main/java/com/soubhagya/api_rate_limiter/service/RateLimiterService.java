package com.soubhagya.api_rate_limiter.service;

import com.soubhagya.api_rate_limiter.config.RateLimiterProperties;
import com.soubhagya.api_rate_limiter.exception.RateLimitExceededException;
import com.soubhagya.api_rate_limiter.model.RateLimitResponse;
import com.soubhagya.api_rate_limiter.model.RateLimitStatusResponse;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class RateLimiterService {

	private static final String KEY_PATTERN = "%s:%s";

	@SuppressWarnings("rawtypes")
	private static final DefaultRedisScript<List> RATE_LIMIT_SCRIPT = new DefaultRedisScript<>();

	static {
		RATE_LIMIT_SCRIPT.setLocation(new ClassPathResource("scripts/rate-limit.lua"));
		RATE_LIMIT_SCRIPT.setResultType(List.class);
	}

	private final StringRedisTemplate redisTemplate;
	private final RateLimiterProperties properties;

	public RateLimiterService(StringRedisTemplate redisTemplate, RateLimiterProperties properties) {
		this.redisTemplate = redisTemplate;
		this.properties = properties;
	}

	public RateLimitResponse consume(String clientIp) {
		return consume(clientIp, properties.getMaxRequests(), properties.getWindowSeconds());
	}

	public RateLimitResponse consume(String clientIp, int maxRequests, int windowSeconds) {
		if (maxRequests <= 0) {
			maxRequests = properties.getMaxRequests();
		}
		if (windowSeconds <= 0) {
			windowSeconds = properties.getWindowSeconds();
		}

		String key = buildKey(clientIp);

		List<?> result = redisTemplate.execute(RATE_LIMIT_SCRIPT, List.of(key),
				String.valueOf(maxRequests), String.valueOf(windowSeconds));

		long count = asLong(result.get(0));
		long ttl = asLong(result.get(1));
		long allowed = asLong(result.get(2));
		long remaining = asLong(result.get(3));

		if (allowed == 0L) {
			long retryAfterSeconds = Math.max(ttl, 0L);
			throw new RateLimitExceededException(retryAfterSeconds);
		}

		return RateLimitResponse.allowed(remaining);
	}

	public RateLimitStatusResponse getStatus(String clientIp) {
		String key = buildKey(clientIp);
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

	private String buildKey(String clientIp) {
		return String.format(KEY_PATTERN, properties.getKeyPrefix(), clientIp);
	}

	private static long asLong(Object value) {
		if (value instanceof Number) {
			return ((Number) value).longValue();
		}
		return Long.parseLong(value.toString());
	}

}