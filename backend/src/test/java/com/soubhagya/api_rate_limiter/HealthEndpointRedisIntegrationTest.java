package com.soubhagya.api_rate_limiter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "management.endpoint.health.show-components=always")
@AutoConfigureMockMvc
class HealthEndpointRedisIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

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

	private static MockHttpServletRequestBuilder requestWithClientIp(String path, String clientIp) {
		return get(path).with(request -> {
			request.setRemoteAddr(clientIp);
			return request;
		});
	}

	@Test
	@EnabledIf("redisRunning")
	void healthReportsUpAndRedisUpWithLiveRedis() throws Exception {
		mockMvc.perform(get("/actuator/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"))
				.andExpect(jsonPath("$.components.redis.status").value("UP"));
	}

	@Test
	@EnabledIf("redisRunning")
	void repeatedHealthChecksDoNotConsumeOrModifyTheRateLimitCounter() throws Exception {
		String clientIp = "health-" + UUID.randomUUID();

		mockMvc.perform(requestWithClientIp("/api/rate-limit/status", clientIp))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.used").value(0))
				.andExpect(jsonPath("$.remaining").value(5));

		for (int i = 0; i < 20; i++) {
			mockMvc.perform(get("/actuator/health")).andReturn();
		}

		mockMvc.perform(requestWithClientIp("/api/rate-limit/status", clientIp))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.used").value(0))
				.andExpect(jsonPath("$.remaining").value(5))
				.andExpect(jsonPath("$.status").value("READY"));
	}

}