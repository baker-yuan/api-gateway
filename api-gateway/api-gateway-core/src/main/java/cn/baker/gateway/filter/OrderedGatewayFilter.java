package cn.baker.gateway.filter;

import org.springframework.core.Ordered;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 将GatewayFilter包装成有序的GatewayFilter
 */
public class OrderedGatewayFilter implements GatewayFilter, Ordered {
	/**
	 * 代理Filter，干活的Filter
	 */
	private final GatewayFilter delegate;

	/**
	 * 过滤器的顺序，在过滤器链中，过滤器的执行顺序由`order`属性决定，`order`值越小，过滤器越先执行。
	 */
	private final int order;

	public OrderedGatewayFilter(GatewayFilter delegate, int order) {
		this.delegate = delegate;
		this.order = order;
	}

	public GatewayFilter getDelegate() {
		return delegate;
	}

	@Override
	public String named() {
		return delegate.named();
	}

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		return this.delegate.filter(exchange, chain);
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	@Override
	public String toString() {
		return "[" + delegate + ", order = " + order + "]";
	}

}
