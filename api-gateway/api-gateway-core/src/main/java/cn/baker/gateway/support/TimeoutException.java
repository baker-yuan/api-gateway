package cn.baker.gateway.support;

import org.springframework.web.bind.annotation.ResponseStatus;

import static org.springframework.http.HttpStatus.GATEWAY_TIMEOUT;

@ResponseStatus(value = GATEWAY_TIMEOUT, reason = "Response took longer than configured timeout")
public class TimeoutException extends Exception {

	public TimeoutException() {
	}

	public TimeoutException(String message) {
		super(message);
	}

	/**
	 * Disables fillInStackTrace for performance reasons.
	 * @return this
	 */
	@Override
	public synchronized Throwable fillInStackTrace() {
		return this;
	}

}
