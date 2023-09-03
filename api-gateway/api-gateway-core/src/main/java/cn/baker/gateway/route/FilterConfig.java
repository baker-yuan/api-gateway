package cn.baker.gateway.route;

/**
 * 过滤器的配置类
 */
public class FilterConfig {
    /**
     * 过滤器的唯一ID
     */
    private String id;

    /**
     * 过滤器的配置信息描述，json string  {timeout: 500}  {balance: rr}
     */
    private String config;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getConfig() {
        return config;
    }

    public void setConfig(String config) {
        this.config = config;
    }
}