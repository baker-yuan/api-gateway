package cn.baker.gateway.support;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.baker.gateway.filter.GlobalFilter;

/**
 * `NameUtils`主要用于处理和操作名称字符串。
 * 这个类提供了一些静态方法，例如：
 * 1. `normalizeRouteName(String name)`：这个方法用于将给定的路由名称规范化，即将所有的非字母数字字符替换为连字符，并将所有的大写字母转换为小写字母。
 * 2. `normalizeName(Class clazz)`：这个方法用于将给定的类名规范化，即将所有的非字母数字字符替换为连字符，并将所有的大写字母转换为小写字母。
 * 3. `generateName(int length)`：这个方法用于生成一个指定长度的随机名称，名称中的字符都是小写字母。
 * 这个类的主要作用是帮助处理和操作名称字符串，使其满足特定的格式要求。
 */
public final class NameUtils {

	private NameUtils() {
		throw new AssertionError("Must not instantiate utility class.");
	}

	/**
	 * Generated name prefix.
	 */
	public static final String GENERATED_NAME_PREFIX = "_genkey_";

	private static final Pattern NAME_PATTERN = Pattern.compile("([A-Z][a-z0-9]+)");

	public static String generateName(int i) {
		return GENERATED_NAME_PREFIX + i;
	}




	public static String normalizeGlobalFilterName(Class<? extends GlobalFilter> clazz) {
		return removeGarbage(clazz.getSimpleName().replace(GlobalFilter.class.getSimpleName(), "")).replace("Filter", "");
	}



	public static String normalizeGlobalFilterNameAsProperty(Class<? extends GlobalFilter> filterClass) {
		return normalizeToCanonicalPropertyFormat(normalizeGlobalFilterName(filterClass));
	}

	public static String normalizeToCanonicalPropertyFormat(String name) {
		Matcher matcher = NAME_PATTERN.matcher(name);
		StringBuffer stringBuffer = new StringBuffer();
		while (matcher.find()) {
			if (stringBuffer.length() != 0) {
				matcher.appendReplacement(stringBuffer, "-" + matcher.group(1));
			}
			else {
				matcher.appendReplacement(stringBuffer, matcher.group(1));
			}
		}
		return stringBuffer.toString().toLowerCase();
	}

	private static String removeGarbage(String s) {
		int garbageIdx = s.indexOf("$Mockito");
		if (garbageIdx > 0) {
			return s.substring(0, garbageIdx);
		}
		return s;
	}

}