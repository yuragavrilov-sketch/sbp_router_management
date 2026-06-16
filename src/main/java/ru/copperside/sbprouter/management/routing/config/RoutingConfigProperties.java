package ru.copperside.sbprouter.management.routing.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "routing")
public record RoutingConfigProperties(String topic) {
    public RoutingConfigProperties {
        if (topic == null) topic = "sbp-router-routing-config";
    }
}
