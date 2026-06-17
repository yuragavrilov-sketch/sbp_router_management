package ru.copperside.sbprouter.management.traffic.adapter.in.messaging;

import ru.copperside.sbprouter.management.traffic.domain.TrafficDirection;
import ru.copperside.sbprouter.management.traffic.domain.TrafficEvent;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

public class TrafficEventMapper {

    public TrafficEvent map(String key, Map<String, String> headers, byte[] body) {
        TrafficDirection direction = "response".equalsIgnoreCase(headers.get("direction"))
                ? TrafficDirection.RESPONSE : TrafficDirection.REQUEST;
        String correlationId = blankToNull(headers.get("correlationId"));
        String txId = blankToNull(headers.get("txId"));
        if (correlationId == null && txId == null) {
            txId = key; // last-resort key fallback
        }
        String operationId = blankToNull(headers.get("operationId"));
        String operationType = blankToNull(headers.get("operationType"));
        return new TrafficEvent(
                direction,
                txId,
                correlationId,
                operationId,
                operationType,
                blankToNull(headers.get("requestType")),
                blankToNull(headers.get("env")),
                parseInstant(headers.get("timestamp")),
                blankToNull(headers.get("terminalOwner")),
                blankToNull(headers.get("route")),
                blankToNull(headers.get("upstream")),
                blankToNull(headers.get("outcome")),
                body == null ? null : new String(body, StandardCharsets.UTF_8));
    }

    private static Instant parseInstant(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (Exception e) {
            return null;
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
