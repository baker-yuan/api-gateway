package cn.baker.gateway.filter;

import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 内部全局过滤器
 */
public interface GlobalFilter {
	/**
	 * 插件名称，全局唯一
	 */
	String named();

	/**
	 * 处理Web请求，并（可选地）通过给定的 {@link GatewayFilterChain} 委托给下一个 {@code GatewayFilter}
	 *
	 * @param exchange 当前的服务器交换信息
	 * @param chain 提供了一种方式来委托给下一个过滤器
	 * @return {@code Mono<Void>} 表示请求处理完成的信号
	 */
	Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain);
}