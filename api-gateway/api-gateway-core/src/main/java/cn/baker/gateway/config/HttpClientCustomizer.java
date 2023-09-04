package cn.baker.gateway.config;

import reactor.netty.http.client.HttpClient;

@FunctionalInterface
public interface HttpClientCustomizer {
	/**
	 * 自定义指定的 {@link HttpClient}.
	 *
	 * @param httpClient 需要自定义的http客户端
	 * @return 自定义后的HttpClient
	 */
	HttpClient customize(HttpClient httpClient);
}