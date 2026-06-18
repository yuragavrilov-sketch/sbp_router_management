package ru.copperside.sbprouter.management.traffic.application;

import org.junit.jupiter.api.Test;
import ru.copperside.sbprouter.management.traffic.application.port.out.TrafficWriteRepository;
import ru.copperside.sbprouter.management.traffic.domain.TrafficDirection;
import ru.copperside.sbprouter.management.traffic.domain.TrafficEvent;
import ru.copperside.sbprouter.management.traffic.domain.TrafficStatus;
import ru.copperside.sbprouter.management.traffic.domain.TrafficTransaction;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TrafficIngestServiceTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-05-29T09:00:05Z"), ZoneOffset.UTC);
    private final FakeRepo repo = new FakeRepo();
    private final TrafficIngestService service = new TrafficIngestService(repo, clock);

    private TrafficEvent request(Instant ts) {
        return new TrafficEvent(TrafficDirection.REQUEST, "tx-1", "corr-1", null, null, "ReqAuthPay",
                "local", ts, "owner-A", "route-x", null, null, "<req/>");
    }

    private TrafficEvent response(Instant ts) {
        return new TrafficEvent(TrafficDirection.RESPONSE, "tx-1", "corr-1", null, null, "ReqAuthPay",
                "local", ts, null, null, "infosrv", "Code=0", "<ans/>");
    }

    @Test
    void requestThenResponseCorrelatesIntoOneRespondedRow() {
        service.record(request(Instant.parse("2026-05-29T09:00:00Z")));
        service.record(response(Instant.parse("2026-05-29T09:00:00.040Z")));

        assertThat(repo.store).hasSize(1);
        TrafficTransaction t = repo.store.get("corr-1");
        assertThat(t.status()).isEqualTo(TrafficStatus.RESPONDED);
        assertThat(t.requestXml()).isEqualTo("<req/>");
        assertThat(t.responseXml()).isEqualTo("<ans/>");
        assertThat(t.upstream()).isEqualTo("infosrv");
        assertThat(t.terminalOwner()).isEqualTo("owner-A");
        assertThat(t.latencyMs()).isEqualTo(40L);
    }

    @Test
    void responseThenRequestAlsoCorrelates() {
        service.record(response(Instant.parse("2026-05-29T09:00:00.040Z")));
        service.record(request(Instant.parse("2026-05-29T09:00:00Z")));

        TrafficTransaction t = repo.store.get("corr-1");
        assertThat(t.status()).isEqualTo(TrafficStatus.RESPONDED);
        assertThat(t.latencyMs()).isEqualTo(40L);
        assertThat(t.requestXml()).isEqualTo("<req/>");
        assertThat(t.responseXml()).isEqualTo("<ans/>");
    }

    @Test
    void requestOnlyIsPending() {
        service.record(request(Instant.parse("2026-05-29T09:00:00Z")));
        TrafficTransaction t = repo.store.get("corr-1");
        assertThat(t.status()).isEqualTo(TrafficStatus.PENDING);
        assertThat(t.latencyMs()).isNull();
    }

    /** Fake that reproduces the coalesce-merge + status/latency the SQL upsert performs. */
    static class FakeRepo implements TrafficWriteRepository {
        final Map<String, TrafficTransaction> store = new HashMap<>();

        @Override
        public void upsert(TrafficTransaction p) {
            TrafficTransaction e = store.get(p.correlationId());
            String txId = coalesce(p.txId(), e == null ? null : e.txId());
            String requestType = coalesce(p.requestType(), e == null ? null : e.requestType());
            String terminalOwner = coalesce(p.terminalOwner(), e == null ? null : e.terminalOwner());
            String route = coalesce(p.route(), e == null ? null : e.route());
            String upstream = coalesce(p.upstream(), e == null ? null : e.upstream());
            String outcome = coalesce(p.outcome(), e == null ? null : e.outcome());
            Instant requestAt = coalesce(p.requestAt(), e == null ? null : e.requestAt());
            Instant responseAt = coalesce(p.responseAt(), e == null ? null : e.responseAt());
            String env = coalesce(p.env(), e == null ? null : e.env());
            String requestXml = coalesce(p.requestXml(), e == null ? null : e.requestXml());
            String responseXml = coalesce(p.responseXml(), e == null ? null : e.responseXml());
            boolean hasFault = p.hasFault() || (e != null && e.hasFault());
            String faultString = coalesce(p.faultString(), e == null ? null : e.faultString());
            Long latency = (requestAt != null && responseAt != null)
                    ? Duration.between(requestAt, responseAt).toMillis() : null;
            TrafficStatus status;
            if (responseAt != null) {
                status = hasFault ? TrafficStatus.RESPONDED_WITH_ERROR : TrafficStatus.RESPONDED;
            } else {
                status = TrafficStatus.PENDING;
            }
            Instant createdAt = e == null ? p.createdAt() : e.createdAt();
            String operationId = coalesce(e == null ? null : e.operationId(), p.operationId());
            String operationType = coalesce(e == null ? null : e.operationType(), p.operationType());
            store.put(p.correlationId(), new TrafficTransaction(p.correlationId(), txId, requestType,
                    operationId, operationType,
                    terminalOwner, route, upstream, outcome, status, requestAt, responseAt, latency,
                    env, requestXml, responseXml, createdAt, p.updatedAt(), hasFault, faultString));
        }

        private static <T> T coalesce(T a, T b) {
            return a != null ? a : b;
        }
    }
}
