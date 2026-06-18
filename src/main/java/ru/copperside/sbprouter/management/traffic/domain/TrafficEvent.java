package ru.copperside.sbprouter.management.traffic.domain;

import java.time.Instant;

public record TrafficEvent(
        TrafficDirection direction,
        String txId,
        String correlationId,
        String operationId,
        String operationType,
        String requestType,
        String env,
        Instant timestamp,
        String terminalOwner,
        String route,
        String upstream,
        String outcome,
        String body,
        boolean hasFault,
        String faultString
) {
    /** Convenience constructor for events where fault detection is not applicable (requests). */
    public TrafficEvent(
            TrafficDirection direction,
            String txId,
            String correlationId,
            String operationId,
            String operationType,
            String requestType,
            String env,
            Instant timestamp,
            String terminalOwner,
            String route,
            String upstream,
            String outcome,
            String body) {
        this(direction, txId, correlationId, operationId, operationType, requestType, env,
                timestamp, terminalOwner, route, upstream, outcome, body, false, null);
    }

    public String correlationKey() {
        return correlationId != null && !correlationId.isBlank() ? correlationId : txId;
    }
}
