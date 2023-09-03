package cn.baker.gateway.config;

import reactor.netty.http.client.HttpClient;

@FunctionalInterface
public interface HttpClientCustomizer {

	/**
	 * Customize the specified {@link HttpClient}.
	 * @param httpClient the http client to customize.
	 * @return the customized HttpClient.
	 */
	HttpClient customize(HttpClient httpClient);

}