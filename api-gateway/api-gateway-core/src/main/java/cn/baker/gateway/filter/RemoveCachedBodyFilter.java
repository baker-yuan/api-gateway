package cn.baker.gateway.filter;

import cn.baker.gateway.support.ServerWebExchangeUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Mono;

import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.PooledDataBuffer;
import org.springframework.web.server.ServerWebExchange;

/**
 * AdaptCachedBodyGlobalFilter反向操作，清除缓存
 */
public class RemoveCachedBodyFilter implements GlobalFilter, Ordered {

	private static final Log log = LogFactory.getLog(RemoveCachedBodyFilter.class);

	@Override
	public String named() {
		return null;
	}

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		return chain.filter(exchange).doFinally(s -> {
			Object attribute = exchange.getAttributes().remove(ServerWebExchangeUtils.CACHED_REQUEST_BODY_ATTR);
			if (attribute != null && attribute instanceof PooledDataBuffer) {
				PooledDataBuffer dataBuffer = (PooledDataBuffer) attribute;
				if (dataBuffer.isAllocated()) {
					if (log.isTraceEnabled()) {
						log.trace("releasing cached body in exchange attribute");
					}
					dataBuffer.release();
				}
			}
		});
	}

	@Override
	public int getOrder() {
		return HIGHEST_PRECEDENCE;
	}

}