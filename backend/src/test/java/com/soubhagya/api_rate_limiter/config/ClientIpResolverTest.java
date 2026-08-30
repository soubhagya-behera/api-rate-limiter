package com.soubhagya.api_rate_limiter.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class ClientIpResolverTest {

	private final ClientIpResolver clientIpResolver = new ClientIpResolver();

	private MockHttpServletRequest requestWithRemoteAddr(String remoteAddr) {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRemoteAddr(remoteAddr);
		return request;
	}

	@Test
	void usesFirstXForwardedForEntryOverAllOtherSources() {
		MockHttpServletRequest request = requestWithRemoteAddr("10.0.0.1");
		request.addHeader("X-Forwarded-For", "203.0.113.9, 10.0.0.99");
		request.addHeader("X-Real-IP", "198.51.100.7");

		assertThat(clientIpResolver.resolve(request)).isEqualTo("203.0.113.9");
	}

	@Test
	void trimsWhitespaceAroundFirstForwardedEntry() {
		MockHttpServletRequest request = requestWithRemoteAddr("10.0.0.1");
		request.addHeader("X-Forwarded-For", " 203.0.113.9 , 10.0.0.99 ");

		assertThat(clientIpResolver.resolve(request)).isEqualTo("203.0.113.9");
	}

	@Test
	void usesXRealIpWhenForwardedForIsAbsent() {
		MockHttpServletRequest request = requestWithRemoteAddr("10.0.0.1");
		request.addHeader("X-Real-IP", "198.51.100.7");

		assertThat(clientIpResolver.resolve(request)).isEqualTo("198.51.100.7");
	}

	@Test
	void trimsXRealIp() {
		MockHttpServletRequest request = requestWithRemoteAddr("10.0.0.1");
		request.addHeader("X-Real-IP", " 198.51.100.7 ");

		assertThat(clientIpResolver.resolve(request)).isEqualTo("198.51.100.7");
	}

	@Test
	void fallsBackToRemoteAddrWhenNoForwardingHeadersArePresent() {
		MockHttpServletRequest request = requestWithRemoteAddr("10.0.0.1");

		assertThat(clientIpResolver.resolve(request)).isEqualTo("10.0.0.1");
	}

	@Test
	void ignoresBlankForwardedForAndUsesXRealIp() {
		MockHttpServletRequest request = requestWithRemoteAddr("10.0.0.1");
		request.addHeader("X-Forwarded-For", "   ");
		request.addHeader("X-Real-IP", "198.51.100.7");

		assertThat(clientIpResolver.resolve(request)).isEqualTo("198.51.100.7");
	}

	@Test
	void fallsBackToRemoteAddrWhenBothHeadersAreBlank() {
		MockHttpServletRequest request = requestWithRemoteAddr("10.0.0.1");
		request.addHeader("X-Forwarded-For", " ");
		request.addHeader("X-Real-IP", "   ");

		assertThat(clientIpResolver.resolve(request)).isEqualTo("10.0.0.1");
	}

}