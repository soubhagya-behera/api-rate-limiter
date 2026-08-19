package com.soubhagya.api_rate_limiter.service;

import com.soubhagya.api_rate_limiter.config.RateLimiterProperties;
import com.soubhagya.api_rate_limiter.exception.RateLimitExceededException;
import com.soubhagya.api_rate_limiter.model.RateLimitResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class RateLimiterConcurrencyTest {

	private static final int LIMIT = 5;
	private static final String KEY_PREFIX = "rate-limit:ip";

	@Autowired
	private RateLimiterService rateLimiterService;

	@Autowired
	private StringRedisTemplate redisTemplate;

	static boolean redisRunning() {
		LettuceConnectionFactory factory = new LettuceConnectionFactory("localhost", 6379);
		try {
			factory.afterPropertiesSet();
			RedisConnection connection = factory.getConnection();
			try {
				return "PONG".equals(connection.ping());
			} finally {
				connection.close();
			}
		} catch (Exception e) {
			return false;
		} finally {
			factory.destroy();
		}
	}

	private static String keyFor(String clientIp) {
		return KEY_PREFIX + ":" + clientIp;
	}

	@Test
	@EnabledIf("redisRunning")
	void exactlyFiveOfTwentyConcurrentRequestsAreAllowed() throws Exception {
		String clientIp = "concurrency-" + UUID.randomUUID();
		String key = keyFor(clientIp);
		redisTemplate.delete(key);

		int attempts = 20;
		ExecutorService executor = Executors.newFixedThreadPool(attempts);
		CountDownLatch ready = new CountDownLatch(attempts);
		CountDownLatch start = new CountDownLatch(1);
		List<Future<Object>> futures = new ArrayList<>();

		for (int i = 0; i < attempts; i++) {
			futures.add(executor.submit(() -> {
				ready.countDown();
				start.await();
				try {
					return rateLimiterService.consume(clientIp);
				} catch (RateLimitExceededException e) {
					return e;
				}
			}));
		}

		ready.await(10, TimeUnit.SECONDS);
		start.countDown();

		int allowed = 0;
		int rejected = 0;
		for (Future<Object> future : futures) {
			Object outcome = future.get(30, TimeUnit.SECONDS);
			if (outcome instanceof RateLimitResponse response) {
				allowed++;
				assertThat(response.remainingRequests()).isBetween(0L, (long) LIMIT - 1);
			} else if (outcome instanceof RateLimitExceededException ex) {
				rejected++;
				assertThat(ex.getRetryAfterSeconds()).isGreaterThanOrEqualTo(0L);
			} else {
				throw new AssertionError("Unexpected outcome: " + outcome);
			}
		}

		executor.shutdownNow();

		assertThat(allowed).isEqualTo(LIMIT);
		assertThat(rejected).isEqualTo(attempts - LIMIT);
		assertThat(redisTemplate.opsForValue().get(key)).isEqualTo(String.valueOf(LIMIT));
		assertThat(redisTemplate.getExpire(key, TimeUnit.SECONDS)).isGreaterThan(0L);

		redisTemplate.delete(key);
	}

	@Test
	@EnabledIf("redisRunning")
	void rejectedRequestsDoNotIncreaseTheCounterOrResetTheTtl() throws Exception {
		String clientIp = "ttl-" + UUID.randomUUID();
		String key = keyFor(clientIp);
		redisTemplate.delete(key);

		for (int i = 0; i < LIMIT; i++) {
			assertThat(rateLimiterService.consume(clientIp).success()).isTrue();
		}

		long ttlBefore = redisTemplate.getExpire(key, TimeUnit.SECONDS);

		for (int i = 0; i < 5; i++) {
			assertThatThrownBy(() -> rateLimiterService.consume(clientIp))
					.isInstanceOf(RateLimitExceededException.class);
		}

		Thread.sleep(1100L);
		long ttlAfter = redisTemplate.getExpire(key, TimeUnit.SECONDS);

		assertThat(redisTemplate.opsForValue().get(key)).isEqualTo(String.valueOf(LIMIT));
		assertThat(ttlAfter).isLessThan(ttlBefore);
		assertThat(ttlAfter).isLessThan((long) 60);
		assertThat(ttlAfter).isBetween(0L, 59L);

		redisTemplate.delete(key);
	}

	@Test
	@EnabledIf("redisRunning")
	void newWindowStartsAfterTheTtlExpires() throws Exception {
		String clientIp = "expiry-" + UUID.randomUUID();
		String key = keyFor(clientIp);
		redisTemplate.delete(key);

		RateLimiterProperties shortWindow = new RateLimiterProperties();
		shortWindow.setMaxRequests(LIMIT);
		shortWindow.setWindowSeconds(1);
		RateLimiterService shortWindowService = new RateLimiterService(redisTemplate, shortWindow);

		for (int i = 0; i < LIMIT; i++) {
			assertThat(shortWindowService.consume(clientIp).success()).isTrue();
		}
		assertThatThrownBy(() -> shortWindowService.consume(clientIp))
				.isInstanceOf(RateLimitExceededException.class);

		Thread.sleep(1500L);

		RateLimitResponse response = shortWindowService.consume(clientIp);

		assertThat(response.success()).isTrue();
		assertThat(response.remainingRequests()).isEqualTo(LIMIT - 1);
		assertThat(redisTemplate.opsForValue().get(key)).isEqualTo("1");

		redisTemplate.delete(key);
	}

}