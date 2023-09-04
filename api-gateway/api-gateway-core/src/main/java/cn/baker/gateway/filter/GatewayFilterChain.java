package cn.baker.gateway.filter;

import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 处理和管理GatewayFilter的执行顺序
 */
public interface GatewayFilterChain {
	/**
	 * 在过滤器链中，将处理任务委托给下一个 {@code GatewayFilter}
	 *
	 * @param exchange 当前的服务器交换信息
	 * @return {@code Mono<Void>} 请求处理完成的信号
	 */
	Mono<Void> filter(ServerWebExchange exchange);
}
