package ru.copperside.sbprouter.management.traffic.adapter.in.web;

import ru.copperside.sbprouter.management.traffic.domain.TrafficTransaction;

import java.time.Instant;

public record TrafficTransactionResponse(
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
        String env,
        String requestXml,
        String responseXml,
        String faultString
) {
    public static TrafficTransactionResponse from(TrafficTransaction t) {
        return new TrafficTransactionResponse(t.correlationId(), t.txId(), t.requestType(),
                t.operationId(), t.operationType(),
                t.terminalOwner(), t.route(), t.upstream(), t.outcome(), t.status().name(),
                t.requestAt(), t.responseAt(), t.latencyMs(), t.env(), t.requestXml(), t.responseXml(),
                t.faultString());
    }
}
