package cn.baker.gateway.config;

import cn.baker.gateway.config.conditional.ConditionalOnEnabledGlobalFilter;
import cn.baker.gateway.filter.GatewayFilter;
import cn.baker.gateway.filter.GlobalFilter;
import cn.baker.gateway.filter.complete.http.NettyRoutingFilter;
import cn.baker.gateway.filter.complete.http.NettyWriteResponseFilter;
import cn.baker.gateway.filter.headers.HttpHeadersFilter;
import cn.baker.gateway.handler.FilteringWebHandler;
import cn.baker.gateway.handler.RoutePredicateHandlerMapping;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.autoconfigure.web.embedded.NettyWebServerFactoryCustomizer;
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory;
import cn.baker.gateway.filter.AdaptCachedBodyGlobalFilter;
import cn.baker.gateway.filter.RemoveCachedBodyFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import cn.baker.gateway.support.FilterManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.reactive.DispatcherHandler;
import reactor.netty.http.client.HttpClient;

import java.util.List;

/**
 * 网关自动配置类
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "spring.cloud.gateway.enabled", matchIfMissing = true)
@EnableConfigurationProperties
@ConditionalOnClass(DispatcherHandler.class)
public class GatewayAutoConfiguration {

    @Bean
    public GatewayProperties gatewayProperties() {
        return new GatewayProperties();
    }

    @Bean
    public FilterManager filterManager(List<GlobalFilter> globalFilters, List<GatewayFilter> filters) {
        return new FilterManager(globalFilters, filters);
    }

    @Bean
    @ConditionalOnMissingBean
    public FilteringWebHandler filteringWebHandler(FilterManager filterManager) {
        return new FilteringWebHandler(filterManager);
    }

    @Bean
    @ConditionalOnMissingBean
    public RoutePredicateHandlerMapping routePredicateHandlerMapping(FilteringWebHandler filteringWebHandler) {
        return new RoutePredicateHandlerMapping(filteringWebHandler);
    }


    @Bean
    public AdaptCachedBodyGlobalFilter adaptCachedBodyGlobalFilter() {
        return new AdaptCachedBodyGlobalFilter();
    }

    @Bean
    public RemoveCachedBodyFilter removeCachedBodyFilter(){
        return new RemoveCachedBodyFilter();
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(HttpClient.class)
    protected static class NettyConfiguration {
        @Bean
        public HttpClientProperties httpClientProperties() {
            return new HttpClientProperties();
        }

        @Bean
        @ConditionalOnProperty(name = "spring.cloud.gateway.httpserver.wiretap")
        public NettyWebServerFactoryCustomizer nettyServerWiretapCustomizer(Environment environment, ServerProperties serverProperties) {
            return new NettyWebServerFactoryCustomizer(environment, serverProperties) {
                @Override
                public void customize(NettyReactiveWebServerFactory factory) {
                    factory.addServerCustomizers(httpServer -> httpServer.wiretap(true));
                    super.customize(factory);
                }
            };
        }

        @Bean
        public HttpClientSslConfigurer httpClientSslConfigurer(ServerProperties serverProperties, HttpClientProperties httpClientProperties) {
            return new HttpClientSslConfigurer(httpClientProperties.getSsl(), serverProperties) {
            };
        }

        @Bean
        @ConditionalOnMissingBean({ HttpClient.class, HttpClientFactory.class })
        public HttpClientFactory gatewayHttpClientFactory(HttpClientProperties properties, ServerProperties serverProperties, List<HttpClientCustomizer> customizers, HttpClientSslConfigurer sslConfigurer) {
            return new HttpClientFactory(properties, serverProperties, sslConfigurer, customizers);
        }

        @Bean
        @ConditionalOnEnabledGlobalFilter
        public NettyRoutingFilter routingFilter(HttpClient httpClient, ObjectProvider<List<HttpHeadersFilter>> headersFilters, HttpClientProperties properties) {
            return new NettyRoutingFilter(httpClient, headersFilters, properties);
        }

        @Bean
        @ConditionalOnEnabledGlobalFilter(NettyRoutingFilter.class)
        public NettyWriteResponseFilter nettyWriteResponseFilter(GatewayProperties properties) {
            return new NettyWriteResponseFilter(properties.getStreamingMediaTypes());
        }


    }

}
