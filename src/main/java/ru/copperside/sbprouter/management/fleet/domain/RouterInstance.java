package ru.copperside.sbprouter.management.fleet.domain;

import java.time.Instant;
import java.util.List;

/**
 * A point-in-time view of one running sbp-router pod, assembled from its latest heartbeat. Held
 * in-memory only (no persistence) — the fleet is derived state, not a source of truth.
 */
public record RouterInstance(
        String instanceId,
        Instant startedAt,
        Instant lastHeartbeat,
        String activeGroup,
        long routingConfigVersion,
        List<String> groups,
        List<RouterBackend> backends,
        RouterMetrics metrics
) {

    public record RouterBackend(String url, String group, boolean banned) {
    }

    public record RouterMetrics(
            int activeRequests,
            double requestsTotal,
            double upstreamErrorsTotal,
            double kafkaPublishedTotal,
            long requestCount,
            double avgLatencyMs,
            double maxLatencyMs
    ) {
    }
}
