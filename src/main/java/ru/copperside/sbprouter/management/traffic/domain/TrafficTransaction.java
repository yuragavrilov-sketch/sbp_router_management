package ru.copperside.sbprouter.management.traffic.domain;

import java.time.Instant;

public record TrafficTransaction(
        String correlationId,
        String txId,
        String requestType,
        String terminalOwner,
        String route,
        String upstream,
        String outcome,
        TrafficStatus status,
        Instant requestAt,
        Instant responseAt,
        Long latencyMs,
        String env,
        String requestXml,
        String responseXml,
        Instant createdAt,
        Instant updatedAt
) {
}
