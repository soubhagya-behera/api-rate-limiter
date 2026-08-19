package com.soubhagya.api_rate_limiter.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimiterPropertiesValidationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(TestConfig.class);

	@Test
	void bindsValidConfiguration() {
		contextRunner
				.withPropertyValues(
						"rate-limit.max-requests=10",
						"rate-limit.window-seconds=30",
						"rate-limit.key-prefix=custom:prefix")
				.run(context -> {
					assertThat(context).hasNotFailed();
					RateLimiterProperties properties = context.getBean(RateLimiterProperties.class);
					assertThat(properties.getMaxRequests()).isEqualTo(10);
					assertThat(properties.getWindowSeconds()).isEqualTo(30);
					assertThat(properties.getKeyPrefix()).isEqualTo("custom:prefix");
				});
	}

	@Test
	void failsFastWhenMaxRequestsIsZero() {
		contextRunner
				.withPropertyValues("rate-limit.max-requests=0")
				.run(context -> assertThat(context).hasFailed());
	}

	@Test
	void failsFastWhenMaxRequestsIsNegative() {
		contextRunner
				.withPropertyValues("rate-limit.max-requests=-5")
				.run(context -> assertThat(context).hasFailed());
	}

	@Test
	void failsFastWhenWindowSecondsIsZero() {
		contextRunner
				.withPropertyValues("rate-limit.window-seconds=0")
				.run(context -> assertThat(context).hasFailed());
	}

	@Test
	void failsFastWhenWindowSecondsIsNegative() {
		contextRunner
				.withPropertyValues("rate-limit.window-seconds=-10")
				.run(context -> assertThat(context).hasFailed());
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(RateLimiterProperties.class)
	static class TestConfig {
	}

}
