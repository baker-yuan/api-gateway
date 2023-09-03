package cn.baker.gateway.handler;

import cn.baker.gateway.filter.GatewayFilter;
import cn.baker.gateway.filter.GatewayFilterChain;
import cn.baker.gateway.filter.GlobalFilter;
import com.google.common.collect.Lists;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import cn.baker.gateway.filter.OrderedGatewayFilter;
import cn.baker.gateway.route.FilterConfig;
import cn.baker.gateway.route.Route;
import cn.baker.gateway.support.FilterManager;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebHandler;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static cn.baker.gateway.support.ServerWebExchangeUtils.*;


public class FilteringWebHandler implements WebHandler {

    protected static final Log logger = LogFactory.getLog(FilteringWebHandler.class);

    /**
     * 插件管理
     */
    private final FilterManager filterManager;

    public FilteringWebHandler(FilterManager filterManager) {
        this.filterManager = filterManager;
    }

    /**
     * GlobalFilter包装成GatewayFilter
     *
     * @param filters 全局过滤器
     * @return 过滤器链
     */
    private static List<GatewayFilter> loadFilters(List<GlobalFilter> filters) {
        return filters.stream().map(filter -> {
            GatewayFilterAdapter gatewayFilter = new GatewayFilterAdapter(filter);
            if (filter instanceof Ordered) {
                int order = ((Ordered) filter).getOrder();
                return new OrderedGatewayFilter(gatewayFilter, order);
            }
            return gatewayFilter;
        }).collect(Collectors.toList());
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange) {
        Route route = exchange.getRequiredAttribute(GATEWAY_ROUTE_ATTR);
        List<FilterConfig> filterConfigs = Lists.newArrayList();

        // 添加过滤器：内部全局过滤器+全局过滤器+路由指定的过滤器放在一起
        // 1、添加内部全局过滤器
        List<GatewayFilter> combined = new ArrayList<>(loadFilters(filterManager.getGlobalFilters()));
        // 2、添加路由指定的过滤器
        combined.addAll(filterManager.getFiltersByConfig(route.getFilterConfigs()));
        filterConfigs.addAll(route.getFilterConfigs());
        // 3、添加全局过滤器
        List<FilterConfig> configGlobalFilters = exchange.getRequiredAttribute(GATEWAY_GLOBAL_FILTER_CONFIG);
        Map<String, GatewayFilter> filterMap = combined.stream().collect(Collectors.toMap(GatewayFilter::named, v -> v, (v1, v2) -> v1));
        for (FilterConfig config : configGlobalFilters) {
            if (!filterMap.containsKey(config.getId())) {
                combined.add(filterManager.getFiltersById(config.getId()));
                filterConfigs.add(config);
            }
        }


        // 插件配置存入上下文
        exchange.getAttributes().put(GATEWAY_FILTER_CONFIG_MAP, filterConfigs.stream().collect(Collectors.toMap(FilterConfig::getId, v->v, (v1, v2)->v1)));

        // 排序
        AnnotationAwareOrderComparator.sort(combined);

        if (logger.isDebugEnabled()) {
            logger.debug("Sorted gatewayFilterFactories: " + combined);
        }

        return new DefaultGatewayFilterChain(combined).filter(exchange);
    }


    /**
     * `GatewayFilter`的执行链
     */
    private static class DefaultGatewayFilterChain implements GatewayFilterChain {
        private final int index;
        private final List<GatewayFilter> filters;
        // private GatewayFilter completeFilter;

        DefaultGatewayFilterChain(List<GatewayFilter> filters) {
            this.filters = filters;
            this.index = 0;
        }
        private DefaultGatewayFilterChain(DefaultGatewayFilterChain parent, int index) {
            this.filters = parent.getFilters();
            this.index = index;
        }
        public List<GatewayFilter> getFilters() {
            return filters;
        }

        @Override
        public Mono<Void> filter(ServerWebExchange exchange) {
            return Mono.defer(() -> {
                if (this.index < filters.size()) {
                    GatewayFilter filter = filters.get(this.index);
                    DefaultGatewayFilterChain chain = new DefaultGatewayFilterChain(this, this.index + 1);
                    return filter.filter(exchange, chain);
                }
                // else if (this.index == filters.size()) {
                //
                // }
                else {
                    return Mono.empty(); // complete
                }
            });
        }
    }

    /**
     * 适配器，将`GatewayFilter`适配为`WebFilter`
     */
    private static class GatewayFilterAdapter implements GatewayFilter {
        private final GlobalFilter delegate;
        GatewayFilterAdapter(GlobalFilter delegate) {
            this.delegate = delegate;
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
        public String toString() {
            return "GatewayFilterAdapter{" + "delegate=" + delegate + '}';
        }
    }


}