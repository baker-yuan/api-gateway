package cn.baker.gateway.route;

import org.springframework.core.Ordered;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Route implements Ordered {
    private final String id;
    private final String uri;
    private final Integer order;
    private final List<FilterConfig> filterConfigs;
    private final Map<String, Object> metadata;
    /**
     * 上游数据
     * 1、路由直接关联上游
     * 2、路由关联服务ID，服务直接关联上游
     * 3、路由关联服务ID，服务关联上游ID
     */
    private final Upstream upstream;

    public Route(String id, String uri, Integer order, List<FilterConfig> filterConfigs, Upstream upstream, Map<String, Object> metadata) {
        this.id = id;
        this.uri = uri;
        this.order = order;
        this.filterConfigs = filterConfigs;
        this.upstream = upstream;
        this.metadata = metadata;
    }

    @Override
    public int getOrder() {
        return order;
    }


    public List<FilterConfig> getFilterConfigs() {
        return filterConfigs;
    }

    public Map<String, Object> getMetadata() {
        return Collections.unmodifiableMap(metadata);
    }

    public String getId() {
        return id;
    }
}