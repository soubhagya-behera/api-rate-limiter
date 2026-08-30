package com.soubhagya.api_rate_limiter.config;

import com.soubhagya.api_rate_limiter.annotation.RateLimited;
import com.soubhagya.api_rate_limiter.model.RateLimitResponse;
import com.soubhagya.api_rate_limiter.service.RateLimiterService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RateLimitedInterceptor implements HandlerInterceptor {

	
	public static final String RESPONSE_ATTRIBUTE = RateLimitedInterceptor.class.getName() + ".response";

	private final RateLimiterService rateLimiterService;
	private final ClientIpResolver clientIpResolver;

	public RateLimitedInterceptor(RateLimiterService rateLimiterService, ClientIpResolver clientIpResolver) {
		this.rateLimiterService = rateLimiterService;
		this.clientIpResolver = clientIpResolver;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		if (!(handler instanceof HandlerMethod handlerMethod)) {
			return true;
		}

		RateLimited rateLimited = resolveRateLimited(handlerMethod);
		if (rateLimited == null) {
			return true;
		}

		RateLimitResponse rateLimitResponse = rateLimiterService.consume(
				clientIpResolver.resolve(request), rateLimited.maxRequests(), rateLimited.windowSeconds());
		RateLimitHeaders.apply(response, rateLimitResponse);
		request.setAttribute(RESPONSE_ATTRIBUTE, rateLimitResponse);
		return true;
	}

	public static RateLimitResponse responseFrom(HttpServletRequest request) {
		return (RateLimitResponse) request.getAttribute(RESPONSE_ATTRIBUTE);
	}

	private static RateLimited resolveRateLimited(HandlerMethod handlerMethod) {
		RateLimited annotation = handlerMethod.getMethodAnnotation(RateLimited.class);
		if (annotation == null) {
			annotation = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getBeanType(), RateLimited.class);
		}
		return annotation;
	}

}