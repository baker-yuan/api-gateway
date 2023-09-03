package cn.baker.gateway.filter.headers;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.web.server.ServerWebExchange;

public interface HttpHeadersFilter {

	static HttpHeaders filterRequest(List<HttpHeadersFilter> filters, ServerWebExchange exchange) {
		HttpHeaders headers = exchange.getRequest().getHeaders();
		return filter(filters, headers, exchange, Type.REQUEST);
	}

	static HttpHeaders filter(List<HttpHeadersFilter> filters, HttpHeaders input, ServerWebExchange exchange, Type type) {
		if (filters != null) {
			HttpHeaders filtered = input;
			for (int i = 0; i < filters.size(); i++) {
				HttpHeadersFilter filter = filters.get(i);
				if (filter.supports(type)) {
					filtered = filter.filter(filtered, exchange);
				}
			}
			return filtered;
		}
		return input;
	}

	/**
	 * Filters a set of Http Headers.
	 * @param input Http Headers
	 * @param exchange a {@link ServerWebExchange} that should be filtered
	 * @return filtered Http Headers
	 */
	HttpHeaders filter(HttpHeaders input, ServerWebExchange exchange);

	default boolean supports(Type type) {
		return type.equals(Type.REQUEST);
	}

	enum Type {
		/**
		 * Filter for request headers.
		 */
		REQUEST,
		/**
		 * Filter for response headers.
		 */
		RESPONSE
	}

}
