package cn.baker.gateway.support;

import cn.baker.gateway.filter.GatewayFilter;
import cn.baker.gateway.filter.GlobalFilter;
import cn.baker.gateway.route.FilterConfig;
import com.google.common.collect.Lists;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FilterManager {
    /**
     * 全局过滤器
     */
    private final List<GlobalFilter> globalFilters;

    /**
     * 过滤器
     */
    private final List<GatewayFilter> filters;
    private final Map<String, GatewayFilter> filterMap;

    public FilterManager(List<GlobalFilter> globalFilters, List<GatewayFilter> filters) {
        this.globalFilters = globalFilters;
        this.filters = filters;

        this.filterMap = filters.stream().collect(Collectors.toMap(GatewayFilter::named, v -> v, (v1, v2) -> v1));
    }


    public List<GlobalFilter> getGlobalFilters() {
        return globalFilters;
    }

    public List<GatewayFilter> getFilters() {
        return filters;
    }


    public List<GatewayFilter> getFiltersByConfig(List<FilterConfig> filterConfigs) {
        List<GatewayFilter> res = Lists.newArrayListWithCapacity(filterConfigs.size());
        for (FilterConfig config : filterConfigs) {
            GatewayFilter filter = filterMap.get(config.getId());
            if (filter == null) {
                continue;
            }
            res.add(filter);
        }
        return res;
    }

    public GatewayFilter getFiltersById(String id) {
        return filterMap.get(id);
    }
}
