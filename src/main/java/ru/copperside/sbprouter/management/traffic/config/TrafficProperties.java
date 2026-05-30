package ru.copperside.sbprouter.management.traffic.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "traffic")
public record TrafficProperties(
        Kafka kafka,
        Integer retentionDays
) {
    public TrafficProperties {
        if (kafka == null) {
            kafka = new Kafka(false, "localhost:9092", "sbp-router-traffic", "sbp-router-management", "sbp-router-manifest");
        }
        if (retentionDays == null) {
            retentionDays = 30;
        }
    }

    public record Kafka(Boolean enabled, String bootstrapServers, String topic, String groupId, String manifestTopic) {
        public Kafka {
            if (enabled == null) {
                enabled = false;
            }
            if (bootstrapServers == null) {
                bootstrapServers = "localhost:9092";
            }
            if (topic == null) {
                topic = "sbp-router-traffic";
            }
            if (groupId == null) {
                groupId = "sbp-router-management";
            }
            if (manifestTopic == null) {
                manifestTopic = "sbp-router-manifest";
            }
        }
    }
}
