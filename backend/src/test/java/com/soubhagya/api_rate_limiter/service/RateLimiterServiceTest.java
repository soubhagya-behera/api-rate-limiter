package com.soubhagya.api_rate_limiter.service;

import com.soubhagya.api_rate_limiter.config.RateLimiterProperties;
import com.soubhagya.api_rate_limiter.exception.RateLimitExceededException;
import com.soubhagya.api_rate_limiter.model.RateLimitResponse;
import com.soubhagya.api_rate_limiter.model.RateLimitStatusResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateLimiterServiceTest {

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
	void allowsRequestWhenCountIsBelowLimitAndSetsExpirationOnNewKey() {
		when(valueOperations.increment(KEY)).thenReturn(1L);

		RateLimitResponse response = service.consume(CLIENT_IP);

		assertThat(response.success()).isTrue();
		assertThat(response.message()).isEqualTo("Request allowed");
		assertThat(response.remainingRequests()).isEqualTo(4);
		assertThat(response.retryAfterSeconds()).isNull();
		verify(redisTemplate).expire(KEY, Duration.ofSeconds(60));
	}

	@Test
	void allowsRequestWhenCountIsExactlyAtTheLimit() {
		when(valueOperations.increment(KEY)).thenReturn(5L);

		RateLimitResponse response = service.consume(CLIENT_IP);

		assertThat(response.success()).isTrue();
		assertThat(response.remainingRequests()).isZero();
		verify(redisTemplate, never()).expire(KEY, Duration.ofSeconds(60));
	}

	@Test
	void doesNotResetExpirationForExistingKeys() {
		when(valueOperations.increment(KEY)).thenReturn(3L);

		service.consume(CLIENT_IP);

		verify(redisTemplate, never()).expire(KEY, Duration.ofSeconds(60));
	}

	@Test
	void rejectsRequestWhenCountExceedsTheLimit() {
		when(valueOperations.increment(KEY)).thenReturn(6L);
		when(redisTemplate.getExpire(KEY, TimeUnit.SECONDS)).thenReturn(42L);

		assertThatThrownBy(() -> service.consume(CLIENT_IP))
				.isInstanceOf(RateLimitExceededException.class)
				.hasMessage("Too many requests")
				.satisfies(ex -> assertThat(((RateLimitExceededException) ex).getRetryAfterSeconds()).isEqualTo(42L));
	}

	@Test
	void rejectedRequestRollsBackCounterToTheLimit() {
		when(valueOperations.increment(KEY)).thenReturn(6L);
		when(redisTemplate.getExpire(KEY, TimeUnit.SECONDS)).thenReturn(42L);

		assertThatThrownBy(() -> service.consume(CLIENT_IP))
				.isInstanceOf(RateLimitExceededException.class);

		verify(valueOperations).decrement(KEY);
	}

	@Test
	void allowedRequestAtTheLimitDoesNotRollBackCounter() {
		when(valueOperations.increment(KEY)).thenReturn(5L);

		service.consume(CLIENT_IP);

		verify(valueOperations, never()).decrement(KEY);
	}

	@Test
	void rejectedRequestDoesNotIncreaseReportedUsedCount() {
		when(valueOperations.increment(KEY)).thenReturn(6L);
		when(valueOperations.decrement(KEY)).thenReturn(5L);
		when(redisTemplate.getExpire(KEY, TimeUnit.SECONDS)).thenReturn(42L);

		assertThatThrownBy(() -> service.consume(CLIENT_IP))
				.isInstanceOf(RateLimitExceededException.class);

		when(valueOperations.get(KEY)).thenReturn("5");

		RateLimitStatusResponse status = service.getStatus(CLIENT_IP);

		assertThat(status.used()).isEqualTo(5);
		assertThat(status.remaining()).isZero();
		assertThat(status.status()).isEqualTo("LIMIT_REACHED");
	}

}