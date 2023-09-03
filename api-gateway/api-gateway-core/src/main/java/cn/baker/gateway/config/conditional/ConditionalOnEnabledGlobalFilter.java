package cn.baker.gateway.config.conditional;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.baker.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Conditional;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@Documented
@Conditional(OnEnabledGlobalFilter.class)
public @interface ConditionalOnEnabledGlobalFilter {

	/**
	 * The class component to check for.
	 * @return the class that must be enabled
	 */
	Class<? extends GlobalFilter> value() default OnEnabledGlobalFilter.DefaultValue.class;

}
