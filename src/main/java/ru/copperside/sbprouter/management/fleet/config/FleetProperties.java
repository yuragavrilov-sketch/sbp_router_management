package ru.copperside.sbprouter.management.fleet.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Fleet-view config: the heartbeat topic, the consumer group, and the time after which an instance
 * that stopped heartbeating is considered stale.
 */
@ConfigurationProperties(prefix = "fleet")
public record FleetProperties(
        String topic,
        String groupId,
        Duration ttl
) {
    public FleetProperties {
        if (topic == null) {
            topic = "sbp-router-heartbeat";
        }
        if (groupId == null) {
            groupId = "sbp-router-management-fleet";
        }
        if (ttl == null) {
            ttl = Duration.ofSeconds(45);
        }
    }
}
