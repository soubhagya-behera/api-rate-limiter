package com.soubhagya.api_rate_limiter.service;

import com.soubhagya.api_rate_limiter.config.RateLimiterProperties;
import com.soubhagya.api_rate_limiter.model.RateLimitStatusResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateLimiterStatusTest {

	private static final String CLIENT_IP = "127.0.0.1";
	private static final String KEY = "rate-limit:ip:" + CLIENT_IP;

	@Mock
	private StringRedisTemplate redisTemplate;

	@Mock
	private ValueOperations<String, String> valueOperations;

	private RateLimiterService service;

	@BeforeEach
	void setUp() {
		RateLimiterProperties properties = new RateLimiterProperties();
		service = new RateLimiterService(redisTemplate, properties);
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
	}

	@Test
	void reportsReadyWhenNoKeyExists() {
		when(valueOperations.get(KEY)).thenReturn(null);

		RateLimitStatusResponse response = service.getStatus(CLIENT_IP);

		assertThat(response.limit()).isEqualTo(5);
		assertThat(response.used()).isZero();
		assertThat(response.remaining()).isEqualTo(5);
		assertThat(response.windowSeconds()).isEqualTo(60);
		assertThat(response.resetInSeconds()).isZero();
		assertThat(response.status()).isEqualTo("READY");
		verify(redisTemplate, never()).getExpire(KEY, TimeUnit.SECONDS);
	}

	@Test
	void reportsActiveWindowWithTtl() {
		when(valueOperations.get(KEY)).thenReturn("2");
		when(redisTemplate.getExpire(KEY, TimeUnit.SECONDS)).thenReturn(42L);

		RateLimitStatusResponse response = service.getStatus(CLIENT_IP);

		assertThat(response.used()).isEqualTo(2);
		assertThat(response.remaining()).isEqualTo(3);
		assertThat(response.resetInSeconds()).isEqualTo(42);
		assertThat(response.status()).isEqualTo("ACTIVE");
	}

	@Test
	void reportsLimitReachedWhenUsedEqualsLimit() {
		when(valueOperations.get(KEY)).thenReturn("5");
		when(redisTemplate.getExpire(KEY, TimeUnit.SECONDS)).thenReturn(30L);

		RateLimitStatusResponse response = service.getStatus(CLIENT_IP);

		assertThat(response.used()).isEqualTo(5);
		assertThat(response.remaining()).isZero();
		assertThat(response.resetInSeconds()).isEqualTo(30);
		assertThat(response.status()).isEqualTo("LIMIT_REACHED");
	}

	@Test
	void clampsRemainingToZeroWhenUsedExceedsLimit() {
		when(valueOperations.get(KEY)).thenReturn("7");

		RateLimitStatusResponse response = service.getStatus(CLIENT_IP);

		assertThat(response.remaining()).isZero();
		assertThat(response.status()).isEqualTo("LIMIT_REACHED");
	}

	@Test
	void computesRemainingAsLimitMinusUsed() {
		when(valueOperations.get(KEY)).thenReturn("3");
		when(redisTemplate.getExpire(KEY, TimeUnit.SECONDS)).thenReturn(15L);

		RateLimitStatusResponse response = service.getStatus(CLIENT_IP);

		assertThat(response.remaining()).isEqualTo(2);
		assertThat(response.resetInSeconds()).isEqualTo(15);
	}

	@Test
	void doesNotIncrementTheCounter() {
		when(valueOperations.get(KEY)).thenReturn("1");

		service.getStatus(CLIENT_IP);

		verify(valueOperations, never()).increment(KEY);
	}

}