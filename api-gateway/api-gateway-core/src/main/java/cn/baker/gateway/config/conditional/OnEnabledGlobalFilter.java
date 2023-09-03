package cn.baker.gateway.config.conditional;

import reactor.core.publisher.Mono;

import cn.baker.gateway.filter.GatewayFilterChain;
import cn.baker.gateway.filter.GlobalFilter;
import cn.baker.gateway.support.NameUtils;
import org.springframework.web.server.ServerWebExchange;

public class OnEnabledGlobalFilter extends OnEnabledComponent<GlobalFilter> {

	@Override
	protected String normalizeComponentName(Class<? extends GlobalFilter> filterClass) {
		return "global-filter." + NameUtils.normalizeGlobalFilterNameAsProperty(filterClass);
	}

	@Override
	protected Class<?> annotationClass() {
		return ConditionalOnEnabledGlobalFilter.class;
	}

	@Override
	protected Class<? extends GlobalFilter> defaultValueClass() {
		return DefaultValue.class;
	}

	static class DefaultValue implements GlobalFilter {

		@Override
		public String named() {
			return null;
		}

		@Override
		public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
			throw new UnsupportedOperationException("class DefaultValue is never meant to be intantiated");
		}

	}

}