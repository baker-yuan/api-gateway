package cn.baker.gateway.support;

/**
 * 定义与路由元数据相关的常量
 */
public final class RouteMetadataUtils {
	private RouteMetadataUtils() {
		throw new AssertionError("Must not instantiate utility class.");
	}

	/**
	 * 响应超时的属性名
	 */
	public static final String RESPONSE_TIMEOUT_ATTR = "response-timeout";

	/**
	 * 连接超时的属性名
	 */
	public static final String CONNECT_TIMEOUT_ATTR = "connect-timeout";

}
