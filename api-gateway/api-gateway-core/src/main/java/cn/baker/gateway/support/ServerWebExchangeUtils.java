package cn.baker.gateway.support;

import io.netty.buffer.Unpooled;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import cn.baker.gateway.route.FilterConfig;
import org.springframework.core.io.buffer.*;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.function.Function;

public final class ServerWebExchangeUtils {
    private static final Log log = LogFactory.getLog(ServerWebExchangeUtils.class);
    /**
     * 路由数据
     */
    public static final String GATEWAY_ROUTE_ATTR = qualify("gatewayRoute");
    /**
     * 上游信息
     */
    public static final String GATEWAY_UPSTREAM = qualify("gatewayUpstream");
    /**
     * 配置的全局过滤器
     */
    public static final String GATEWAY_GLOBAL_FILTER_CONFIG = qualify("gatewayGlobalFilterConfig");


    /**
     * 插件配置
     * map key=filterId value=FilterConfig
     */
    public static final String GATEWAY_FILTER_CONFIG_MAP = qualify("gatewayFilterConfig");



    /**
     * 缓存请求体的key。当调用 {@link #cacheRequestBodyAndRequest(ServerWebExchange, Function)} 或者
     * {@link #cacheRequestBody(ServerWebExchange, Function)}
     */
    public static final String CACHED_REQUEST_BODY_ATTR = "cachedRequestBody";
    /**
     * 缓存ServerHttpRequestDecorator。当调用 {@link #cacheRequestBodyAndRequest(ServerWebExchange, Function)}
     */
    public static final String CACHED_SERVER_HTTP_REQUEST_DECORATOR_ATTR = "cachedServerHttpRequestDecorator";






    /**
     * 客户端响应元数据（如状态码和头部信息）
     * reactor.netty.http.client.HttpClientResponse
     */
    public static final String CLIENT_RESPONSE_ATTR = qualify("gatewayClientResponse");
    /**
     * 客户端响应连接
     * reactor.netty.Connection
     */
    public static final String CLIENT_RESPONSE_CONN_ATTR = qualify("gatewayClientResponseConnection");
    /**
     * 处理后的客户端响应头
     */
    public static final String CLIENT_RESPONSE_HEADER_NAMES = qualify("gatewayClientResponseHeaderNames");
    /**
     * 客户端原始响应的Content-Type
     */
    public static final String ORIGINAL_RESPONSE_CONTENT_TYPE_ATTR = "original_response_content_type";

    /**
     * 下游url地址
     */
    public static final String GATEWAY_REQUEST_URL_ATTR = qualify("gatewayRequestUrl");
    /**
     * 标记是否需要保留原始请求的Host头。
     * 当你在路由定义中设置了`preserveHostHeader`属性为`true`，那么在转发请求到下游服务时，Gateway会保留原始请求的Host头。这在某些情况下是有用的，比如下游服务需要根据Host头进行一些特殊处理。
     * 在`NettyRoutingFilter`的`filter`方法中，会检查这个属性，如果它的值为`true`，那么在设置请求头时，会将原始请求的Host头添加到请求头中。
     */
    public static final String PRESERVE_HOST_HEADER_ATTRIBUTE = qualify("preserveHostHeader");
    /**
     * 标记一个路由过滤器已经成功被调用。这允许用户编写自定义的路由过滤器，这些过滤器可以禁用内置的路由过滤器。
     */
    public static final String GATEWAY_ALREADY_ROUTED_ATTR = qualify("gatewayAlreadyRouted");


    private static final byte[] EMPTY_BYTES = {};


    /**
     * 在ServerWebExchange属性中缓存请求体，和创建 {@link ServerHttpRequestDecorator} 这些属性分别是
     * {@link #CACHED_REQUEST_BODY_ATTR} 和
     * {@link #CACHED_SERVER_HTTP_REQUEST_DECORATOR_ATTR}
     *
     * @param exchange 可用的ServerWebExchange
     * @param function 一个接受创建的ServerHttpRequestDecorator的函数
     * @param <T> 返回的 {@link Mono} 泛型类型
     * @return 由函数参数创建的类型为T的Mono
     */
    public static <T> Mono<T> cacheRequestBodyAndRequest(ServerWebExchange exchange, Function<ServerHttpRequest, Mono<T>> function) {
        return cacheRequestBody(exchange, true, function);
    }
    /**
     * 在ServerWebExchange属性中缓存请求体。该属性是 {@link #CACHED_REQUEST_BODY_ATTR}.
     *
     * @param exchange 可用的ServerWebExchange
     * @param function 一个接受创建的ServerHttpRequestDecorator的函数
     * @param <T> 返回的 {@link Mono} 泛型类型
     * @return 由函数参数创建的类型为T的Mono
     */
    public static <T> Mono<T> cacheRequestBody(ServerWebExchange exchange, Function<ServerHttpRequest, Mono<T>> function) {
        return cacheRequestBody(exchange, false, function);
    }

    private static <T> Mono<T> cacheRequestBody(ServerWebExchange exchange, boolean cacheDecoratedRequest, Function<ServerHttpRequest, Mono<T>> function) {
        ServerHttpResponse response = exchange.getResponse();
        DataBufferFactory factory = response.bufferFactory();
        // Join all the DataBuffers so we have a single DataBuffer for the body
        return DataBufferUtils
                .join(exchange.getRequest().getBody())
                .defaultIfEmpty(factory.wrap(EMPTY_BYTES))
                .map(dataBuffer -> decorate(exchange, dataBuffer, cacheDecoratedRequest))
                .switchIfEmpty(Mono.just(exchange.getRequest()))
                .flatMap(function);
    }

    private static ServerHttpRequest decorate(ServerWebExchange exchange, DataBuffer dataBuffer, boolean cacheDecoratedRequest) {
        // 检查DataBuffer对象的可读字节数是否大于0。如果大于0将DataBuffer对象缓存到ServerWebExchange的属性中，以便后续使用
        if (dataBuffer.readableByteCount() > 0) {
            if (log.isTraceEnabled()) {
                log.trace("retaining body in exchange attribute");
            }
            Object cachedDataBuffer = exchange.getAttribute(CACHED_REQUEST_BODY_ATTR);
            // don't cache if body is already cached
            if (!(cachedDataBuffer instanceof DataBuffer)) {
                exchange.getAttributes().put(CACHED_REQUEST_BODY_ATTR, dataBuffer);
            }
        }

        // 建了一个ServerHttpRequestDecorator对象，包装了ServerWebExchange中的原始请求。
        // 在这个装饰器中，重写了getBody方法，使得每次调用getBody方法时，都会返回一个新的DataBuffer对象，该对象是原始DataBuffer对象的副本
        ServerHttpRequest decorator = new ServerHttpRequestDecorator(exchange.getRequest()) {
            @Override
            public Flux<DataBuffer> getBody() {
                return Mono.fromSupplier(() -> {
                    if (exchange.getAttribute(CACHED_REQUEST_BODY_ATTR) == null) {
                        // probably == downstream closed or no body
                        return null;
                    }
                    if (dataBuffer instanceof NettyDataBuffer) {
                        NettyDataBuffer pdb = (NettyDataBuffer) dataBuffer;
                        return pdb.factory().wrap(pdb.getNativeBuffer().retainedSlice());
                    }
                    else if (dataBuffer instanceof DefaultDataBuffer) {
                        DefaultDataBuffer ddf = (DefaultDataBuffer) dataBuffer;
                        return ddf.factory().wrap(Unpooled.wrappedBuffer(ddf.getNativeBuffer()).nioBuffer());
                    }
                    else {
                        throw new IllegalArgumentException("Unable to handle DataBuffer of type " + dataBuffer.getClass());
                    }
                }).flux();
            }
        };
        if (cacheDecoratedRequest) {
            exchange.getAttributes().put(CACHED_SERVER_HTTP_REQUEST_DECORATOR_ATTR, decorator);
        }
        return decorator;
    }

    /**
     * 获取插件配置
     *
     * @param exchange
     * @param filterID 插件id
     * @return 插件配置
     */
    public <T> T filterConfig(ServerWebExchange exchange, Class<T> filterConfigClass,  String filterID) {
        Map<String, FilterConfig> filterConfigMap = exchange.getAttribute(GATEWAY_FILTER_CONFIG_MAP);
        FilterConfig filterConfig = filterConfigMap.get(filterID);
        if (filterConfig == null) {
            return null;
        }
        String configStr = filterConfig.getConfig();
        T result = null;
        try {
            result = JSONUtil.parse(configStr, filterConfigClass);
        } catch (Exception e) {
        }
        return result;
    }



    public static boolean isAlreadyRouted(ServerWebExchange exchange) {
        return exchange.getAttributeOrDefault(GATEWAY_ALREADY_ROUTED_ATTR, false);
    }

    public static void setAlreadyRouted(ServerWebExchange exchange) {
        exchange.getAttributes().put(GATEWAY_ALREADY_ROUTED_ATTR, true);
    }

    private static String qualify(String attr) {
        return ServerWebExchangeUtils.class.getName() + "." + attr;
    }
}