package ru.copperside.sbprouter.management.traffic.adapter.in.web;

import ru.copperside.sbprouter.management.traffic.domain.TrafficTransaction;

import java.time.Instant;

public record TrafficTransactionSummaryResponse(
        String correlationId,
        String txId,
        String requestType,
        String operationId,
        String operationType,
        String terminalOwner,
        String route,
        String upstream,
        String outcome,
        String status,
        Instant requestAt,
        Instant responseAt,
        Long latencyMs,
        String env
) {
    public static TrafficTransactionSummaryResponse from(TrafficTransaction t) {
        return new TrafficTransactionSummaryResponse(
                t.correlationId(), t.txId(), t.requestType(),
                t.operationId(), t.operationType(),
                t.terminalOwner(), t.route(), t.upstream(), t.outcome(), t.status().name(),
                t.requestAt(), t.responseAt(), t.latencyMs(), t.env());
    }
}
