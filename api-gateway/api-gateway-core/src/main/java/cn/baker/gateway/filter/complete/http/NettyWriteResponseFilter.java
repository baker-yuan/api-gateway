package cn.baker.gateway.filter.complete.http;

import java.util.List;

import cn.baker.gateway.filter.GatewayFilterChain;
import io.netty.buffer.ByteBuf;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import cn.baker.gateway.filter.GlobalFilter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.Connection;

import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.web.server.ServerWebExchange;

import static cn.baker.gateway.support.ServerWebExchangeUtils.CLIENT_RESPONSE_CONN_ATTR;

/**
 * 将从下游服务接收到的响应写回到客户端
 */
public class NettyWriteResponseFilter implements GlobalFilter, Ordered {
	private static final Log log = LogFactory.getLog(NettyWriteResponseFilter.class);

	/**
	 * 后置过滤器，这里排序是比较靠前的
	 */
	public static final int WRITE_RESPONSE_FILTER_ORDER = -1;

	private final List<MediaType> streamingMediaTypes;

	public NettyWriteResponseFilter(List<MediaType> streamingMediaTypes) {
		this.streamingMediaTypes = streamingMediaTypes;
	}

	@Override
	public int getOrder() {
		return WRITE_RESPONSE_FILTER_ORDER;
	}

	@Override
	public String named() {
		return null;
	}

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		// NOTICE: nothing in "pre" filter stage as CLIENT_RESPONSE_CONN_ATTR is not added
		// until the NettyRoutingFilter is run
		// 后置过滤器
		return chain.filter(exchange)
				.then(Mono.defer(() -> {
					// Connection是网关和下游服务之间的连接，是NettyRoutingFilter放进去的
					Connection connection = exchange.getAttribute(CLIENT_RESPONSE_CONN_ATTR);
					if (connection == null) {
						return Mono.empty();
					}
					if (log.isTraceEnabled()) {
						log.trace("NettyWriteResponseFilter start inbound: " + connection.channel().id().asShortText() + ", outbound: " + exchange.getLogPrefix());
					}
					ServerHttpResponse response = exchange.getResponse();
					final Flux<DataBuffer> body = connection
							.inbound()
							.receive()
							.retain()
							.map(byteBuf -> wrap(byteBuf, response));
					MediaType contentType = null;
					try {
						contentType = response.getHeaders().getContentType();
					}
					catch (Exception e) {
						if (log.isTraceEnabled()) {
							log.trace("invalid media type", e);
						}
					}
					return (isStreamingMediaType(contentType) ? response.writeAndFlushWith(body.map(Flux::just)) : response.writeWith(body));
				})).doOnCancel(() -> cleanup(exchange))
				//
				.doOnError(throwable -> cleanup(exchange));
	}

	protected DataBuffer wrap(ByteBuf byteBuf, ServerHttpResponse response) {
		DataBufferFactory bufferFactory = response.bufferFactory();
		if (bufferFactory instanceof NettyDataBufferFactory factory) {
			return factory.wrap(byteBuf);
		}
		// MockServerHttpResponse creates these
		else if (bufferFactory instanceof DefaultDataBufferFactory) {
			DataBuffer buffer = ((DefaultDataBufferFactory) bufferFactory).allocateBuffer(byteBuf.readableBytes());
			buffer.write(byteBuf.nioBuffer());
			byteBuf.release();
			return buffer;
		}
		throw new IllegalArgumentException("Unknown DataBufferFactory type " + bufferFactory.getClass());
	}

	private void cleanup(ServerWebExchange exchange) {
		Connection connection = exchange.getAttribute(CLIENT_RESPONSE_CONN_ATTR);
		if (connection != null && connection.channel().isActive()) {
			connection.dispose();
		}
	}

	private boolean isStreamingMediaType(@Nullable MediaType contentType) {
		if (contentType != null) {
            for (MediaType streamingMediaType : streamingMediaTypes) {
                if (streamingMediaType.isCompatibleWith(contentType)) {
                    return true;
                }
            }
		}
		return false;
	}

}
