package com.soubhagya.api_rate_limiter.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/**
 * Resolves the effective client IP for rate limiting.
 *
 * The backend is reached through the frontend nginx proxy (and, in production,
 * Render's edge), so the immediate TCP peer seen by the container
 * ({@link HttpServletRequest#getRemoteAddr()} is a proxy hop rather than the
 * actual client. nginx already forwards the real client IP in the
 * {@code X-Forwarded-For} / {@code X-Real-IP} headers; this resolver lets the
 * rate limiter key consistently on the real client IP instead of a
 * per-connection proxy address.
 *
 * Resolution order:
 * <ol>
 *     <li>the first entry of {@code X-Forwarded-For} (leftmost = closest to the client)</li>
 *     <li>{@code X-Real-IP}</li>
 *     <li>{@link HttpServletRequest#getRemoteAddr()}</li>
 * </ol>
 */
@Component
public class ClientIpResolver {

	public String resolve(HttpServletRequest request) {
		String firstForwardedIp = firstEntry(request.getHeader("X-Forwarded-For"));
		if (firstForwardedIp != null) {
			return firstForwardedIp;
		}

		String realIp = request.getHeader("X-Real-IP");
		if (realIp != null && !realIp.isBlank()) {
			return realIp.trim();
		}

		return request.getRemoteAddr();
	}

	private static String firstEntry(String headerValue) {
		if (headerValue == null || headerValue.isBlank()) {
			return null;
		}
		String first = headerValue.split(",")[0].trim();
		return first.isEmpty() ? null : first;
	}

}