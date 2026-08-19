package com.soubhagya.api_rate_limiter.service;

import com.soubhagya.api_rate_limiter.config.RateLimiterProperties;
import com.soubhagya.api_rate_limiter.exception.RateLimitExceededException;
import com.soubhagya.api_rate_limiter.model.RateLimitResponse;
import com.soubhagya.api_rate_limiter.model.RateLimitStatusResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
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
	}

	private RateLimiterService serviceWith(int maxRequests, int windowSeconds, String keyPrefix) {
		RateLimiterProperties properties = new RateLimiterProperties();
		properties.setMaxRequests(maxRequests);
		properties.setWindowSeconds(windowSeconds);
		properties.setKeyPrefix(keyPrefix);
		return new RateLimiterService(redisTemplate, properties);
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private void stubExecute(Object count, Object ttl, Object allowed, Object remaining) {
		when(redisTemplate.execute(any(RedisScript.class), anyList(), any(), any()))
				.thenReturn(List.of(count, ttl, allowed, remaining));
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private RateLimitResponse consumeAndCaptureArguments(List<String> keysOut, List<Object> argsOut) {
		ArgumentCaptor<List> keysCaptor = ArgumentCaptor.forClass(List.class);
		ArgumentCaptor<Object> firstArgCaptor = ArgumentCaptor.forClass(Object.class);
		ArgumentCaptor<Object> secondArgCaptor = ArgumentCaptor.forClass(Object.class);

		RateLimitResponse response = service.consume(CLIENT_IP);

		verify(redisTemplate).execute(any(RedisScript.class), keysCaptor.capture(),
				firstArgCaptor.capture(), secondArgCaptor.capture());
		keysOut.addAll(keysCaptor.getValue());
		argsOut.add(firstArgCaptor.getValue());
		argsOut.add(secondArgCaptor.getValue());
		return response;
	}

	@Test
	void firstRequestStartsTheWindowAndIsAllowed() {
		stubExecute(1L, 60L, 1L, 4L);

		List<String> keys = new ArrayList<>();
		List<Object> args = new ArrayList<>();
		RateLimitResponse response = consumeAndCaptureArguments(keys, args);

		assertThat(response.success()).isTrue();
		assertThat(response.message()).isEqualTo("Request allowed");
		assertThat(response.remainingRequests()).isEqualTo(4);
		assertThat(response.retryAfterSeconds()).isNull();
		assertThat(keys).containsExactly(KEY);
		assertThat(args).containsExactly("5", "60");
	}

	@Test
	void allowsRequestsOneThroughFiveWithDecreasingRemaining() {
		for (int count = 1; count <= 5; count++) {
			stubExecute((long) count, 60L, 1L, (long) (5 - count));

			RateLimitResponse response = service.consume(CLIENT_IP);

			assertThat(response.success()).isTrue();
			assertThat(response.remainingRequests()).isEqualTo(5 - count);
		}
	}

	@Test
	void allowsRequestExactlyAtTheLimitWithZeroRemaining() {
		stubExecute(5L, 30L, 1L, 0L);

		RateLimitResponse response = service.consume(CLIENT_IP);

		assertThat(response.success()).isTrue();
		assertThat(response.remainingRequests()).isZero();
	}

	@Test
	void rejectsTheSixthRequestAndReportsRetryAfterFromTtl() {
		stubExecute(5L, 37L, 0L, 0L);

		assertThatThrownBy(() -> service.consume(CLIENT_IP))
				.isInstanceOf(RateLimitExceededException.class)
				.hasMessage("Too many requests")
				.satisfies(ex -> assertThat(((RateLimitExceededException) ex).getRetryAfterSeconds()).isEqualTo(37L));
	}

	@Test
	void rejectedRequestDoesNotIncreaseReportedUsedCount() {
		stubExecute(5L, 42L, 0L, 0L);

		assertThatThrownBy(() -> service.consume(CLIENT_IP))
				.isInstanceOf(RateLimitExceededException.class);

		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.get(KEY)).thenReturn("5");
		when(redisTemplate.getExpire(KEY, TimeUnit.SECONDS)).thenReturn(42L);

		RateLimitStatusResponse status = service.getStatus(CLIENT_IP);

		assertThat(status.used()).isEqualTo(5);
		assertThat(status.remaining()).isZero();
		assertThat(status.status()).isEqualTo("LIMIT_REACHED");
	}

	@Test
	void remainingNeverBecomesNegativeForAllowedRequests() {
		stubExecute(5L, 30L, 1L, 0L);

		RateLimitResponse response = service.consume(CLIENT_IP);

		assertThat(response.success()).isTrue();
		assertThat(response.remainingRequests()).isZero();
	}

	@Test
	void rejectedRequestReportsZeroRemaining() {
		stubExecute(5L, 30L, 0L, 0L);

		assertThatThrownBy(() -> service.consume(CLIENT_IP))
				.isInstanceOf(RateLimitExceededException.class);
	}

	@Test
	void clampsNegativeTtlToZeroForRetryAfter() {
		stubExecute(5L, -1L, 0L, 0L);

		assertThatThrownBy(() -> service.consume(CLIENT_IP))
				.satisfies(ex -> assertThat(((RateLimitExceededException) ex).getRetryAfterSeconds()).isZero());
	}

	@Test
	void allowsConfiguredMaxRequests() {
		service = serviceWith(10, 60, "rate-limit:ip");
		stubExecute(10L, 60L, 1L, 0L);

		RateLimitResponse response = service.consume(CLIENT_IP);

		assertThat(response.success()).isTrue();
		assertThat(response.remainingRequests()).isZero();
	}

	@Test
	void usesConfiguredLimitAndWindowAsScriptArguments() {
		service = serviceWith(7, 30, "custom:prefix");
		stubExecute(1L, 30L, 1L, 6L);

		List<String> keys = new ArrayList<>();
		List<Object> args = new ArrayList<>();
		consumeAndCaptureArguments(keys, args);

		assertThat(keys).containsExactly("custom:prefix:" + CLIENT_IP);
		assertThat(args).containsExactly("7", "30");
	}

	@Test
	void usesConfiguredKeyPrefixForTheKey() {
		service = serviceWith(5, 60, "custom:prefix");
		stubExecute(1L, 60L, 1L, 4L);

		List<String> keys = new ArrayList<>();
		List<Object> args = new ArrayList<>();
		consumeAndCaptureArguments(keys, args);

		assertThat(keys).containsExactly("custom:prefix:" + CLIENT_IP);
	}

	@Test
	void parsesNumericResultsRegardlessOfNumberType() {
		stubExecute(1, 60, 1, 4);

		RateLimitResponse response = service.consume(CLIENT_IP);

		assertThat(response.success()).isTrue();
		assertThat(response.remainingRequests()).isEqualTo(4);
	}

}