package com.soubhagya.api_rate_limiter;

import com.soubhagya.api_rate_limiter.service.RateLimiterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.health.registry.HealthContributorRegistry;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisServerCommands;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
		"spring.autoconfigure.exclude=org.springframework.boot.data.redis.autoconfigure.health.DataRedisReactiveHealthContributorAutoConfiguration",
		"management.endpoints.web.exposure.include=health,info",
		"management.endpoint.health.show-components=always",
		"info.app.name=api-rate-limiter",
		"info.app.description=Redis-backed API rate limiting service"
})
@AutoConfigureMockMvc
class HealthEndpointTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private HealthContributorRegistry healthContributorRegistry;

	@MockitoBean
	private RateLimiterService rateLimiterService;

	@MockitoBean
	private LettuceConnectionFactory redisConnectionFactory;

	private RedisConnection connection;

	private RedisServerCommands serverCommands;

	@BeforeEach
	void stubRedisUp() {
		connection = mock(RedisConnection.class);
		serverCommands = mock(RedisServerCommands.class);
		when(redisConnectionFactory.getConnection()).thenReturn(connection);
		when(connection.serverCommands()).thenReturn(serverCommands);
		Properties info = new Properties();
		info.setProperty("redis_version", "7.0.0");
		when(serverCommands.info()).thenReturn(info);
	}

	@Test
	void healthEndpointReportsUpAndRedisUpWhenRedisIsAvailable() throws Exception {
		mockMvc.perform(get("/actuator/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"))
				.andExpect(jsonPath("$.components.redis.status").value("UP"));
	}

	@Test
	void healthEndpointReportsDownWhenRedisIsUnavailable() throws Exception {
		when(redisConnectionFactory.getConnection())
				.thenThrow(new RedisConnectionFailureException("redis unavailable"));

		mockMvc.perform(get("/actuator/health"))
				.andExpect(status().isServiceUnavailable())
				.andExpect(jsonPath("$.status").value("DOWN"))
				.andExpect(jsonPath("$.components.redis.status").value("DOWN"));
	}

	@Test
	void healthEndpointDoesNotConsumeRateLimitRequests() throws Exception {
		for (int i = 0; i < 10; i++) {
			mockMvc.perform(get("/actuator/health")).andReturn();
		}

		verify(rateLimiterService, never()).consume(anyString());
		verify(rateLimiterService, never()).consume(anyString(), anyInt(), anyInt());
		verify(rateLimiterService, never()).getStatus(anyString());
	}

	@Test
	void healthEndpointDoesNotEmitRateLimitHeaders() throws Exception {
		mockMvc.perform(get("/actuator/health"))
				.andExpect(header().doesNotExist("X-RateLimit-Limit"))
				.andExpect(header().doesNotExist("X-RateLimit-Remaining"))
				.andExpect(header().doesNotExist("X-RateLimit-Reset"))
				.andExpect(header().doesNotExist("Retry-After"));
	}

	@Test
	void redisHealthIndicatorIsRegistered() {
		assertThat(healthContributorRegistry.getContributor("redis")).isNotNull();
	}

	@Test
	void infoEndpointReportsApplicationNameAndDescription() throws Exception {
		mockMvc.perform(get("/actuator/info"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.app.name").value("api-rate-limiter"))
				.andExpect(jsonPath("$.app.description").value("Redis-backed API rate limiting service"));
	}

}