package cn.baker.gateway.event;

import org.springframework.context.ApplicationEvent;

public class EnableBodyCachingEvent extends ApplicationEvent {

	private final String routeId;

	public EnableBodyCachingEvent(Object source, String routeId) {
		super(source);
		this.routeId = routeId;
	}

	public String getRouteId() {
		return this.routeId;
	}

}
