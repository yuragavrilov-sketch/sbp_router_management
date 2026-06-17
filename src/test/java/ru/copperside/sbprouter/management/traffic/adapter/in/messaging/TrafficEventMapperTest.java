package ru.copperside.sbprouter.management.traffic.adapter.in.messaging;

import org.junit.jupiter.api.Test;
import ru.copperside.sbprouter.management.traffic.domain.TrafficDirection;
import ru.copperside.sbprouter.management.traffic.domain.TrafficEvent;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TrafficEventMapperTest {

    private final TrafficEventMapper mapper = new TrafficEventMapper();

    @Test
    void mapsRequestHeadersAndBody() {
        Map<String, String> headers = Map.of(
                "direction", "request", "txId", "tx-1", "correlationId", "corr-1",
                "requestType", "ReqAuthPay", "env", "local",
                "timestamp", "2026-05-29T09:00:00Z", "terminalOwner", "owner-A", "route", "route-x");
        TrafficEvent e = mapper.map("corr-1", headers, "<req/>".getBytes(StandardCharsets.UTF_8));

        assertThat(e.direction()).isEqualTo(TrafficDirection.REQUEST);
        assertThat(e.txId()).isEqualTo("tx-1");
        assertThat(e.correlationId()).isEqualTo("corr-1");
        assertThat(e.requestType()).isEqualTo("ReqAuthPay");
        assertThat(e.timestamp()).isEqualTo(Instant.parse("2026-05-29T09:00:00Z"));
        assertThat(e.terminalOwner()).isEqualTo("owner-A");
        assertThat(e.body()).isEqualTo("<req/>");
    }

    @Test
    void mapsResponseDirectionAndUpstreamOutcome() {
        Map<String, String> headers = Map.of(
                "direction", "response", "txId", "tx-1", "requestType", "ReqAuthPay",
                "env", "local", "timestamp", "2026-05-29T09:00:00.040Z", "upstream", "infosrv", "outcome", "Code=0");
        TrafficEvent e = mapper.map("corr-1", headers, "<ans/>".getBytes(StandardCharsets.UTF_8));

        assertThat(e.direction()).isEqualTo(TrafficDirection.RESPONSE);
        assertThat(e.upstream()).isEqualTo("infosrv");
        assertThat(e.outcome()).isEqualTo("Code=0");
    }

    @Test
    void toleratesMissingTimestampAndCorrelationId() {
        Map<String, String> headers = Map.of("direction", "request", "txId", "tx-9", "requestType", "ReqAuthPay");
        TrafficEvent e = mapper.map("tx-9", headers, new byte[0]);
        assertThat(e.timestamp()).isNull();
        assertThat(e.correlationId()).isNull();
        assertThat(e.correlationKey()).isEqualTo("tx-9");
    }

    @Test
    void mapsOperationIdAndOperationType() {
        Map<String, String> headers = new java.util.HashMap<>(Map.of(
                "direction", "request", "txId", "tx-1", "correlationId", "corr-1",
                "requestType", "ReqAuthPay", "env", "local",
                "timestamp", "2026-05-29T09:00:00Z"));
        headers.put("operationId", "A614711381");
        headers.put("operationType", "C2B");
        TrafficEvent e = mapper.map("corr-1", headers, "<req/>".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        assertThat(e.operationId()).isEqualTo("A614711381");
        assertThat(e.operationType()).isEqualTo("C2B");
    }

    @Test
    void operationFieldsNullWhenAbsent() {
        Map<String, String> headers = Map.of(
                "direction", "request", "txId", "tx-2", "correlationId", "corr-2",
                "requestType", "ReqNoticePay");
        TrafficEvent e = mapper.map("corr-2", headers, new byte[0]);

        assertThat(e.operationId()).isNull();
        assertThat(e.operationType()).isNull();
    }
}
