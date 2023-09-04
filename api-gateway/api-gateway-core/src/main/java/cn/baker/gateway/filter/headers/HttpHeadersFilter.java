package cn.baker.gateway.filter.headers;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.web.server.ServerWebExchange;

public interface HttpHeadersFilter {

	/**
	 * 过滤http头部信息
	 *
	 * @param input http头部信息
	 * @param exchange 需要被过滤的 {@link ServerWebExchange}
	 * @return 过滤后的Http头部信息
	 */
	HttpHeaders filter(HttpHeaders input, ServerWebExchange exchange);


	static HttpHeaders filterRequest(List<HttpHeadersFilter> filters, ServerWebExchange exchange) {
		HttpHeaders headers = exchange.getRequest().getHeaders();
		return filter(filters, headers, exchange, Type.REQUEST);
	}

	static HttpHeaders filter(List<HttpHeadersFilter> filters, HttpHeaders input, ServerWebExchange exchange, Type type) {
		if (filters != null) {
			HttpHeaders filtered = input;
            for (HttpHeadersFilter filter : filters) {
                if (filter.supports(type)) {
                    filtered = filter.filter(filtered, exchange);
                }
            }
			return filtered;
		}
		return input;
	}

	default boolean supports(Type type) {
		return type.equals(Type.REQUEST);
	}

	enum Type {
		/**
		 * 用于请求头的过滤器
		 */
		REQUEST,
		/**
		 * 用于响应头的过滤器
		 */
		RESPONSE
	}
}
