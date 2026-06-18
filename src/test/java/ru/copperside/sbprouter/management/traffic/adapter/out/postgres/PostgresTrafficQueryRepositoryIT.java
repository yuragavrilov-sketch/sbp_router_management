package ru.copperside.sbprouter.management.traffic.adapter.out.postgres;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import ru.copperside.sbprouter.management.support.PostgresTestSupport;
import ru.copperside.sbprouter.management.traffic.application.TrafficListResult;
import ru.copperside.sbprouter.management.traffic.application.TrafficQuery;
import ru.copperside.sbprouter.management.traffic.domain.TrafficStats;
import ru.copperside.sbprouter.management.traffic.domain.TrafficStatus;
import ru.copperside.sbprouter.management.traffic.domain.TrafficTransaction;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@DirtiesContext
class PostgresTrafficQueryRepositoryIT extends PostgresTestSupport {

    @Autowired
    JdbcTemplate jdbc;

    private final Instant t0 = Instant.parse("2026-05-29T09:00:00Z");

    private void seed() {
        PostgresTrafficWriteRepository w = new PostgresTrafficWriteRepository(jdbc);
        // responded ReqAuthPay, latency 40ms
        w.upsert(new TrafficTransaction("c1", "tx1", "ReqAuthPay", null, null, "owner-A", "route-x", null, null,
                TrafficStatus.PENDING, t0, null, null, "local", "<req1/>", null, t0, t0, false, null));
        w.upsert(new TrafficTransaction("c1", "tx1", "ReqAuthPay", null, null, null, null, "infosrv", "Code=0",
                TrafficStatus.PENDING, null, t0.plusMillis(40), null, "local", null, "<ans1/>", t0, t0, false, null));
        // responded ReqNoticePay, latency 60ms
        w.upsert(new TrafficTransaction("c2", "tx2", "ReqNoticePay", null, null, "owner-B", "route-y", null, null,
                TrafficStatus.PENDING, t0, null, null, "local", "<req2/>", null, t0, t0, false, null));
        w.upsert(new TrafficTransaction("c2", "tx2", "ReqNoticePay", null, null, null, null, "stub", "Code=0",
                TrafficStatus.PENDING, null, t0.plusMillis(60), null, "local", null, "<ans2/>", t0, t0, false, null));
        // pending ReqAuthPay
        w.upsert(new TrafficTransaction("c3", "tx3", "ReqAuthPay", null, null, "owner-A", "route-x", null, null,
                TrafficStatus.PENDING, t0, null, null, "local", "<req3/>", null, t0, t0, false, null));
    }

    @Test
    void listFiltersByRequestTypeAndPaginates() {
        seed();
        PostgresTrafficQueryRepository repo = new PostgresTrafficQueryRepository(jdbc);
        TrafficListResult all = repo.list(new TrafficQuery(null, null, null, null, null, null, null, null, null, 0, 50));
        assertThat(all.total()).isEqualTo(3);
        TrafficListResult authPay = repo.list(new TrafficQuery("ReqAuthPay", null, null, null, null, null, null, null, null, 0, 50));
        assertThat(authPay.total()).isEqualTo(2);
        // list rows omit raw XML
        assertThat(authPay.items().get(0).requestXml()).isNull();
    }

    @Test
    void findReturnsFullRowWithXml() {
        seed();
        PostgresTrafficQueryRepository repo = new PostgresTrafficQueryRepository(jdbc);
        TrafficTransaction t = repo.find("c1").orElseThrow();
        assertThat(t.status()).isEqualTo(TrafficStatus.RESPONDED);
        assertThat(t.requestXml()).isEqualTo("<req1/>");
        assertThat(t.responseXml()).isEqualTo("<ans1/>");
        assertThat(t.latencyMs()).isEqualTo(40L);
        assertThat(repo.find("missing")).isEmpty();
    }

    @Test
    void statsAggregatesTotalsAndLatency() {
        seed();
        PostgresTrafficQueryRepository repo = new PostgresTrafficQueryRepository(jdbc);
        TrafficStats s = repo.stats(t0.minusSeconds(60), t0.plusSeconds(60));
        assertThat(s.total()).isEqualTo(3);
        assertThat(s.responded()).isEqualTo(2);
        assertThat(s.pending()).isEqualTo(1);
        assertThat(s.byRequestType()).containsEntry("ReqAuthPay", 2L).containsEntry("ReqNoticePay", 1L);
        assertThat(s.byUpstream()).containsEntry("infosrv", 1L).containsEntry("stub", 1L);
        assertThat(s.latencyAvg()).isEqualTo(50L);   // (40 + 60) / 2
        assertThat(s.latencyP95()).isBetween(40L, 60L);
        assertThat(s.throughputPerMinute()).isNotEmpty();
    }

    @Test
    void operationIdFilterAndRoundTrip() {
        PostgresTrafficWriteRepository w = new PostgresTrafficWriteRepository(jdbc);
        w.upsert(new TrafficTransaction("op-c1", "op-tx1", "ReqAuthPay", "A614711381", "C2B",
                "owner-A", "route-x", null, null,
                TrafficStatus.PENDING, t0, null, null, "local", "<req-op/>", null, t0, t0, false, null));
        w.upsert(new TrafficTransaction("op-c2", "op-tx2", "ReqAuthPay", "B9999", "B2C",
                "owner-B", "route-y", null, null,
                TrafficStatus.PENDING, t0, null, null, "local", "<req-op2/>", null, t0, t0, false, null));

        PostgresTrafficQueryRepository repo = new PostgresTrafficQueryRepository(jdbc);

        // filter by operationId
        TrafficListResult result = repo.list(new TrafficQuery(null, null, null, null, null, null, null, "A614711381", null, 0, 50));
        assertThat(result.total()).isEqualTo(1);
        assertThat(result.items().get(0).operationId()).isEqualTo("A614711381");
        assertThat(result.items().get(0).operationType()).isEqualTo("C2B");
    }

    @Test
    void statsWithOnlyPendingRowsHasNullLatency() {
        PostgresTrafficWriteRepository w = new PostgresTrafficWriteRepository(jdbc);
        // Insert a single request-only partial (PENDING, response_at null)
        w.upsert(new TrafficTransaction("pend-1", "tx-pend", "ReqAuthPay", null, null, "owner-Z", "route-z", null, null,
                TrafficStatus.PENDING, t0, null, null, "local", "<req-pend/>", null, t0, t0, false, null));

        PostgresTrafficQueryRepository repo = new PostgresTrafficQueryRepository(jdbc);
        TrafficStats s = repo.stats(t0.minusSeconds(60), t0.plusSeconds(60));

        assertThat(s.total()).isEqualTo(1);
        assertThat(s.pending()).isEqualTo(1);
        assertThat(s.responded()).isEqualTo(0);
        assertThat(s.latencyAvg()).isNull();
        assertThat(s.latencyP95()).isNull();
        assertThat(s.latencyP99()).isNull();
    }
}
