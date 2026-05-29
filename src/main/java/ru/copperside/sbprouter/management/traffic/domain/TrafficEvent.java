package ru.copperside.sbprouter.management.traffic.domain;

import java.time.Instant;

public record TrafficEvent(
        TrafficDirection direction,
        String txId,
        String correlationId,
        String requestType,
        String env,
        Instant timestamp,
        String terminalOwner,
        String route,
        String upstream,
        String outcome,
        String body
) {
    public String correlationKey() {
        return correlationId != null && !correlationId.isBlank() ? correlationId : txId;
    }
}
