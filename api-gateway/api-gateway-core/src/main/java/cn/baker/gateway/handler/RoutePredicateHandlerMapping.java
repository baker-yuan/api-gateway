package cn.baker.gateway.handler;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import cn.baker.gateway.route.Route;
import org.springframework.web.reactive.handler.AbstractHandlerMapping;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.URISyntaxException;

import static cn.baker.gateway.support.ServerWebExchangeUtils.*;

public class RoutePredicateHandlerMapping extends AbstractHandlerMapping {

    private final FilteringWebHandler webHandler;

    public RoutePredicateHandlerMapping(FilteringWebHandler webHandler) {
        this.webHandler = webHandler;
        setOrder(1);
    }

    @Override
    protected Mono<?> getHandlerInternal(ServerWebExchange exchange) {

        // 路由匹配
        exchange.getAttributes().put(GATEWAY_ROUTE_ATTR, new Route("1", null,null, Lists.newArrayList(), null, Maps.newHashMap()));


        // 全局插件配置
        exchange.getAttributes().put(GATEWAY_GLOBAL_FILTER_CONFIG, Lists.newArrayList());


        try {
            exchange.getAttributes().put(GATEWAY_REQUEST_URL_ATTR, new URI("http://appapi.huhudi.com/config/appLaunch"));

        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }


        return Mono.just(webHandler);
    }
}