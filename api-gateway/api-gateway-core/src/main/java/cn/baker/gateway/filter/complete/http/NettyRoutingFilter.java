package cn.baker.gateway.filter.complete.http;

import java.net.URI;
import java.time.Duration;
import java.util.List;

import cn.baker.gateway.filter.GatewayFilterChain;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelOption;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import cn.baker.gateway.filter.GlobalFilter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.HttpClientResponse;

import org.springframework.beans.factory.ObjectProvider;
import cn.baker.gateway.config.HttpClientProperties;
import cn.baker.gateway.filter.headers.HttpHeadersFilter;
import cn.baker.gateway.filter.headers.HttpHeadersFilter.Type;
import cn.baker.gateway.route.Route;
import cn.baker.gateway.support.TimeoutException;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBuffer;
import org.springframework.core.io.buffer.NettyDataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.AbstractServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;

import static cn.baker.gateway.filter.headers.HttpHeadersFilter.filterRequest;
import static cn.baker.gateway.support.RouteMetadataUtils.CONNECT_TIMEOUT_ATTR;
import static cn.baker.gateway.support.RouteMetadataUtils.RESPONSE_TIMEOUT_ATTR;
import static cn.baker.gateway.support.ServerWebExchangeUtils.CLIENT_RESPONSE_ATTR;
import static cn.baker.gateway.support.ServerWebExchangeUtils.CLIENT_RESPONSE_CONN_ATTR;
import static cn.baker.gateway.support.ServerWebExchangeUtils.CLIENT_RESPONSE_HEADER_NAMES;
import static cn.baker.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;
import static cn.baker.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR;
import static cn.baker.gateway.support.ServerWebExchangeUtils.ORIGINAL_RESPONSE_CONTENT_TYPE_ATTR;
import static cn.baker.gateway.support.ServerWebExchangeUtils.PRESERVE_HOST_HEADER_ATTRIBUTE;
import static cn.baker.gateway.support.ServerWebExchangeUtils.isAlreadyRouted;
import static cn.baker.gateway.support.ServerWebExchangeUtils.setAlreadyRouted;

/**
 * 将将请求路由到下游http服务
 */
public class NettyRoutingFilter implements GlobalFilter, Ordered {
	private static final Log log = LogFactory.getLog(NettyRoutingFilter.class);

	/**
	 * 最后一个执行的过滤器
	 */
	public static final int ORDER = Ordered.LOWEST_PRECEDENCE;

	/**
	 * http客户端配置类
	 */
	private final HttpClientProperties properties;
	/**
	 * 基于 Netty 的非阻塞、事件驱动的 HTTP 客户端
	 */
	private final HttpClient httpClient;


	/**
	 * header处理
	 */
	private final ObjectProvider<List<HttpHeadersFilter>> headersFiltersProvider;
	/**
	 * 不要直接使用此headersFilters，请改用getHeadersFilters()
	 */
	private volatile List<HttpHeadersFilter> headersFilters;



	public NettyRoutingFilter(HttpClient httpClient, ObjectProvider<List<HttpHeadersFilter>> headersFiltersProvider, HttpClientProperties properties) {
		this.httpClient = httpClient;
		this.headersFiltersProvider = headersFiltersProvider;
		this.properties = properties;
	}

	public List<HttpHeadersFilter> getHeadersFilters() {
		if (headersFilters == null) {
			headersFilters = headersFiltersProvider.getIfAvailable();
		}
		return headersFilters;
	}

	@Override
	public int getOrder() {
		return ORDER;
	}

	@Override
	public String named() {
		return null;
	}

	@Override
	@SuppressWarnings("Duplicates")
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		// 获取下游url地址
		URI requestUrl = exchange.getRequiredAttribute(GATEWAY_REQUEST_URL_ATTR);
		// 获取请求协议
		String scheme = requestUrl.getScheme();
		// 请求已经被路由 url非http或https
		if (isAlreadyRouted(exchange) || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
			return chain.filter(exchange);
		}
		// 标记请求已经被路由
		setAlreadyRouted(exchange);
		// 获取请求
		ServerHttpRequest request = exchange.getRequest();
		// 获取请求方式
		final HttpMethod method = HttpMethod.valueOf(request.getMethod().name());
		// 将URL对象转换为一个字符串，这个字符串中的所有非ASCII字符和特殊字符都会被转义。
		final String url = requestUrl.toASCIIString();

		// 扩展点，请求头经过HttpHeadersFilter处理
		HttpHeaders filtered = filterRequest(getHeadersFilters(), exchange);
		// 请求头
		final DefaultHttpHeaders httpHeaders = new DefaultHttpHeaders();
		filtered.forEach(httpHeaders::set);

		// 是否需要保留原始请求的Host头
		boolean preserveHost = exchange.getAttributeOrDefault(PRESERVE_HOST_HEADER_ATTRIBUTE, false);
		// 获取路由
		Route route = exchange.getAttribute(GATEWAY_ROUTE_ATTR);

		Flux<HttpClientResponse> responseFlux = getHttpClient(route, exchange)
		// 设置请求头
		.headers(headers -> {
			headers.add(httpHeaders);
			// Will either be set below, or later by Netty
			headers.remove(HttpHeaders.HOST);
			if (preserveHost) {
				String host = request.getHeaders().getFirst(HttpHeaders.HOST);
				headers.add(HttpHeaders.HOST, host);
			}
		})
		// 发起请求
		.request(method).uri(url).send((req, nettyOutbound) -> {
			if (log.isTraceEnabled()) {
				nettyOutbound.withConnection(connection -> log.trace("outbound route: " + connection.channel().id().asShortText() + ", inbound: " + exchange.getLogPrefix()));
			}
			return nettyOutbound.send(request.getBody().map(this::getByteBuf));
		})
		// 处理响应
		.responseConnection((res, connection) -> {
			// 保存响应&链接到上下文
			// Defer committing the response until all route filters have run
			// Put client response as ServerWebExchange attribute and write
			// response later NettyWriteResponseFilter
			exchange.getAttributes().put(CLIENT_RESPONSE_ATTR, res);
			exchange.getAttributes().put(CLIENT_RESPONSE_CONN_ATTR, connection);

			// res：作为客户端，向其他服务器(下游)发送请求后收到的响应
			// response：作为服务器，需要向发起请求的客户端(外部请求)发送的响应
			ServerHttpResponse response = exchange.getResponse();

			// 存储响应头，以便HttpHeadersFilter修改响应头
			// put headers and status so filters can modify the response
			HttpHeaders headers = new HttpHeaders();
			res.responseHeaders().forEach(entry -> headers.add(entry.getKey(), entry.getValue()));
			// 存储Content-Type到上下文
			String contentTypeValue = headers.getFirst(HttpHeaders.CONTENT_TYPE);
			if (StringUtils.hasLength(contentTypeValue)) {
				exchange.getAttributes().put(ORIGINAL_RESPONSE_CONTENT_TYPE_ATTR, contentTypeValue);
			}

			// 将从客户端(res)接收到的HTTP响应状态码设置到服务器(response)的HTTP响应中
			setResponseStatus(res, response);

			// 扩展点，HttpHeadersFilter处理
			// make sure headers filters run after setting status so it is available in response
			HttpHeaders filteredResponseHeaders = HttpHeadersFilter.filter(getHeadersFilters(), headers, exchange, Type.RESPONSE);

			// 处理HTTP响应头中的Transfer-Encoding和Content-Length字段
			// Transfer-Encoding和Content-Length这两个字段是互斥的，
			// Transfer-Encoding字段表示的是传输编码方式，常见的值有chunked，表示数据是以一块一块的形式发送的。
			// Content-Length字段表示的是实体主体的大小
			if (!filteredResponseHeaders.containsKey(HttpHeaders.TRANSFER_ENCODING) && filteredResponseHeaders.containsKey(HttpHeaders.CONTENT_LENGTH)) {
				// It is not valid to have both the transfer-encoding header and
				// the content-length header.
				// Remove the transfer-encoding header in the response if the
				// content-length header is present.
				response.getHeaders().remove(HttpHeaders.TRANSFER_ENCODING);
			}

			// 存储处理后的响应头到上下文
			exchange.getAttributes().put(CLIENT_RESPONSE_HEADER_NAMES, filteredResponseHeaders.keySet());
			// 设置响应头
			response.getHeaders().addAll(filteredResponseHeaders);

			return Mono.just(res);
		});

		// 设置响应超时
		Duration responseTimeout = getResponseTimeout(route);
		if (responseTimeout != null) {
			responseFlux = responseFlux
					.timeout(responseTimeout, Mono.error(new TimeoutException("Response took longer than timeout: " + responseTimeout)))
					.onErrorMap(TimeoutException.class, th -> new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, th.getMessage(), th));
		}

		// 当responseFlux完成后，调用过滤器链的下一个过滤器
		return responseFlux.then(chain.filter(exchange));
	}


	protected ByteBuf getByteBuf(DataBuffer dataBuffer) {
		if (dataBuffer instanceof NettyDataBuffer) {
			NettyDataBuffer buffer = (NettyDataBuffer) dataBuffer;
			return buffer.getNativeBuffer();
		}
		// MockServerHttpResponse creates these
		else if (dataBuffer instanceof DefaultDataBuffer) {
			DefaultDataBuffer buffer = (DefaultDataBuffer) dataBuffer;
			return Unpooled.wrappedBuffer(buffer.getNativeBuffer());
		}
		throw new IllegalArgumentException("Unable to handle DataBuffer of type " + dataBuffer.getClass());
	}

	private void setResponseStatus(HttpClientResponse clientResponse, ServerHttpResponse response) {
		HttpStatus status = HttpStatus.resolve(clientResponse.status().code());
		if (status != null) {
			response.setStatusCode(status);
		}
		else {
			while (response instanceof ServerHttpResponseDecorator) {
				response = ((ServerHttpResponseDecorator) response).getDelegate();
			}
			if (response instanceof AbstractServerHttpResponse) {
				response.setRawStatusCode(clientResponse.status().code());
			}
			else {
				throw new IllegalStateException("Unable to set status code " + clientResponse.status().code() + " on response of type " + response.getClass().getName());
			}
		}
	}

	/**
	 * Creates a new HttpClient with per route timeout configuration. Sub-classes that
	 * override, should call super.getHttpClient() if they want to honor the per route
	 * timeout configuration.
	 * @param route the current route.
	 * @param exchange the current ServerWebExchange.
	 * @return the configured HttpClient.
	 */
	protected HttpClient getHttpClient(Route route, ServerWebExchange exchange) {
		Object connectTimeoutAttr = route.getMetadata().get(CONNECT_TIMEOUT_ATTR);
		if (connectTimeoutAttr != null) {
			Integer connectTimeout = getInteger(connectTimeoutAttr);
			return this.httpClient.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeout);
		}
		return httpClient;
	}



	/**
	 * 响应超时时间，单位是毫秒。这个属性用于设置 HttpClient 等待下游服务响应的超时时间。
	 */
	private Duration getResponseTimeout(Route route) {
		try {
			if (route.getMetadata().containsKey(RESPONSE_TIMEOUT_ATTR)) {
				Long routeResponseTimeout = getLong(route.getMetadata().get(RESPONSE_TIMEOUT_ATTR));
				if (routeResponseTimeout != null && routeResponseTimeout >= 0) {
					return Duration.ofMillis(routeResponseTimeout);
				}
				else {
					return null;
				}
			}
		}
		catch (NumberFormatException e) {
			// ignore number format and use global default
		}
		return properties.getResponseTimeout();
	}


	static Integer getInteger(Object connectTimeoutAttr) {
		Integer connectTimeout;
		if (connectTimeoutAttr instanceof Integer) {
			connectTimeout = (Integer) connectTimeoutAttr;
		}
		else {
			connectTimeout = Integer.parseInt(connectTimeoutAttr.toString());
		}
		return connectTimeout;
	}

	static Long getLong(Object responseTimeoutAttr) {
		Long responseTimeout = null;
		if (responseTimeoutAttr instanceof Number) {
			responseTimeout = ((Number) responseTimeoutAttr).longValue();
		}
		else if (responseTimeoutAttr != null) {
			responseTimeout = Long.parseLong(responseTimeoutAttr.toString());
		}
		return responseTimeout;
	}

}
