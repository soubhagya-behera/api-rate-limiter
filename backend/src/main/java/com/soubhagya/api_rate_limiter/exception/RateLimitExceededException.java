package com.soubhagya.api_rate_limiter.exception;

public class RateLimitExceededException extends RuntimeException {

	private final long retryAfterSeconds;
	private final int limit;
	private final long resetInSeconds;

	public RateLimitExceededException(long retryAfterSeconds) {
		this(retryAfterSeconds, 0, 0L);
	}

	public RateLimitExceededException(long retryAfterSeconds, int limit, long resetInSeconds) {
		super("Too many requests");
		this.retryAfterSeconds = retryAfterSeconds;
		this.limit = limit;
		this.resetInSeconds = resetInSeconds;
	}

	public long getRetryAfterSeconds() {
		return retryAfterSeconds;
	}

	public int getLimit() {
		return limit;
	}

	public long getResetInSeconds() {
		return resetInSeconds;
	}

}