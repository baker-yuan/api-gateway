package cn.baker.gateway.config;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;

import java.util.Arrays;
import java.util.List;

@ConfigurationProperties(GatewayProperties.PREFIX)
@Validated
public class GatewayProperties {
    /**
     * Properties prefix.
     */
    public static final String PREFIX = "spring.cloud.gateway";

    private final Log logger = LogFactory.getLog(getClass());


    private List<MediaType> streamingMediaTypes = Arrays.asList(
            MediaType.TEXT_EVENT_STREAM,
            new MediaType("application", "stream+json"), new MediaType("application", "grpc"),
            new MediaType("application", "grpc+protobuf"), new MediaType("application", "grpc+json")
    );


    public List<MediaType> getStreamingMediaTypes() {
        return streamingMediaTypes;
    }

}
