package ru.copperside.sbprouter.management.traffic.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class TrafficEventTest {

    @Test
    void correlationKeyUsesCorrelationIdWhenPresent() {
        TrafficEvent e = new TrafficEvent(TrafficDirection.REQUEST, "tx-1", "corr-1", "ReqAuthPay",
                "local", Instant.parse("2026-05-29T09:00:00Z"), "owner", "route", null, null, "<xml/>");
        assertThat(e.correlationKey()).isEqualTo("corr-1");
    }

    @Test
    void correlationKeyFallsBackToTxId() {
        TrafficEvent e = new TrafficEvent(TrafficDirection.REQUEST, "tx-1", null, "ReqAuthPay",
                "local", Instant.parse("2026-05-29T09:00:00Z"), "owner", "route", null, null, "<xml/>");
        assertThat(e.correlationKey()).isEqualTo("tx-1");
    }
}
