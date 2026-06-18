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
import ru.copperside.sbprouter.management.traffic.domain.TrafficStatus;
import ru.copperside.sbprouter.management.traffic.domain.TrafficTransaction;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@DirtiesContext
class PostgresTrafficWriteRepositoryIT extends PostgresTestSupport {

    @Autowired
    JdbcTemplate jdbc;

    private final Instant now = Instant.parse("2026-05-29T09:00:05Z");

    private TrafficTransaction requestPartial() {
        return new TrafficTransaction("corr-1", "tx-1", "ReqAuthPay", null, null, "owner-A", "route-x",
                null, null, TrafficStatus.PENDING, Instant.parse("2026-05-29T09:00:00Z"), null, null,
                "local", "<req/>", null, now, now, false, null);
    }

    private TrafficTransaction responsePartial() {
        return new TrafficTransaction("corr-1", "tx-1", "ReqAuthPay", null, null, null, null,
                "infosrv", "Code=0", TrafficStatus.PENDING, null, Instant.parse("2026-05-29T09:00:00.040Z"), null,
                "local", null, "<ans/>", now, now, false, null);
    }

    @Test
    void requestThenResponseUpsertsOneRespondedRowWithLatency() {
        PostgresTrafficWriteRepository repo = new PostgresTrafficWriteRepository(jdbc);
        repo.upsert(requestPartial());
        repo.upsert(responsePartial());

        assertThat(jdbc.queryForObject("select count(*) from traffic_transactions", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select status from traffic_transactions where correlation_id='corr-1'", String.class))
                .isEqualTo("RESPONDED");
        assertThat(jdbc.queryForObject("select latency_ms from traffic_transactions where correlation_id='corr-1'", Long.class))
                .isEqualTo(40L);
        assertThat(jdbc.queryForObject("select upstream from traffic_transactions where correlation_id='corr-1'", String.class))
                .isEqualTo("infosrv");
        assertThat(jdbc.queryForObject("select terminal_owner from traffic_transactions where correlation_id='corr-1'", String.class))
                .isEqualTo("owner-A");
    }

    @Test
    void responseThenRequestAlsoCorrelates() {
        PostgresTrafficWriteRepository repo = new PostgresTrafficWriteRepository(jdbc);
        repo.upsert(responsePartial());
        repo.upsert(requestPartial());

        assertThat(jdbc.queryForObject("select count(*) from traffic_transactions", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select latency_ms from traffic_transactions where correlation_id='corr-1'", Long.class))
                .isEqualTo(40L);
        assertThat(jdbc.queryForObject("select request_xml from traffic_transactions where correlation_id='corr-1'", String.class))
                .isEqualTo("<req/>");
    }

    @Test
    void redeliveryIsIdempotent() {
        PostgresTrafficWriteRepository repo = new PostgresTrafficWriteRepository(jdbc);
        repo.upsert(requestPartial());
        repo.upsert(requestPartial());
        assertThat(jdbc.queryForObject("select count(*) from traffic_transactions", Integer.class)).isEqualTo(1);
    }

    /**
     * TASK 2 regression: a late/duplicate event arriving AFTER a transaction is already RESPONDED
     * must NOT overwrite its existing timestamps or change its status.
     * The upsert has a WHERE tt.status != 'RESPONDED' clause to prevent this.
     */
    @Test
    void lateRequestAfterResponded_doesNotOverwriteTimestamps() {
        PostgresTrafficWriteRepository repo = new PostgresTrafficWriteRepository(jdbc);

        // 1. Normal request → response flow: transaction becomes RESPONDED
        repo.upsert(requestPartial());
        repo.upsert(responsePartial());

        Instant originalRequestAt = jdbc.queryForObject(
                "select request_at from traffic_transactions where correlation_id='corr-1'", Instant.class);
        Instant originalResponseAt = jdbc.queryForObject(
                "select response_at from traffic_transactions where correlation_id='corr-1'", Instant.class);
        Long originalLatency = jdbc.queryForObject(
                "select latency_ms from traffic_transactions where correlation_id='corr-1'", Long.class);

        assertThat(jdbc.queryForObject("select status from traffic_transactions where correlation_id='corr-1'", String.class))
                .isEqualTo("RESPONDED");

        // 2. Late/duplicate request event with a DIFFERENT (bogus) timestamp arrives
        Instant lateTimestamp = Instant.parse("2099-01-01T00:00:00Z");
        TrafficTransaction lateRequest = new TrafficTransaction(
                "corr-1", "tx-1", "ReqAuthPay", null, null, "owner-LATE", "route-LATE",
                null, null, TrafficStatus.PENDING, lateTimestamp, null, null,
                "local", "<late-req/>", null, now, now, false, null);
        repo.upsert(lateRequest);

        // 3. The RESPONDED row must be completely unchanged
        assertThat(jdbc.queryForObject("select status from traffic_transactions where correlation_id='corr-1'", String.class))
                .isEqualTo("RESPONDED");
        assertThat(jdbc.queryForObject("select request_at from traffic_transactions where correlation_id='corr-1'", Instant.class))
                .isEqualTo(originalRequestAt);
        assertThat(jdbc.queryForObject("select response_at from traffic_transactions where correlation_id='corr-1'", Instant.class))
                .isEqualTo(originalResponseAt);
        assertThat(jdbc.queryForObject("select latency_ms from traffic_transactions where correlation_id='corr-1'", Long.class))
                .isEqualTo(originalLatency);
        assertThat(jdbc.queryForObject("select terminal_owner from traffic_transactions where correlation_id='corr-1'", String.class))
                .isEqualTo("owner-A"); // must NOT be overwritten with "owner-LATE"
    }

    @Test
    void operationFieldsPersistedAndCoalescedOnResponse() {
        PostgresTrafficWriteRepository repo = new PostgresTrafficWriteRepository(jdbc);

        // Request carries operationId/operationType; response has nulls
        TrafficTransaction reqWithOp = new TrafficTransaction("corr-op", "tx-op", "ReqAuthPay",
                "A614711381", "C2B", "owner-A", "route-x",
                null, null, TrafficStatus.PENDING, Instant.parse("2026-05-29T09:00:00Z"), null, null,
                "local", "<req/>", null, now, now, false, null);
        TrafficTransaction respNoOp = new TrafficTransaction("corr-op", "tx-op", "ReqAuthPay",
                null, null, null, null,
                "infosrv", "Code=0", TrafficStatus.PENDING, null, Instant.parse("2026-05-29T09:00:00.040Z"), null,
                "local", null, "<ans/>", now, now, false, null);

        repo.upsert(reqWithOp);
        repo.upsert(respNoOp);

        // operation fields must be preserved (not overwritten by response nulls)
        assertThat(jdbc.queryForObject("select operation_id from traffic_transactions where correlation_id='corr-op'", String.class))
                .isEqualTo("A614711381");
        assertThat(jdbc.queryForObject("select operation_type from traffic_transactions where correlation_id='corr-op'", String.class))
                .isEqualTo("C2B");
        assertThat(jdbc.queryForObject("select status from traffic_transactions where correlation_id='corr-op'", String.class))
                .isEqualTo("RESPONDED");
    }

    /**
     * TASK 2 regression: a late response arriving for an already-RESPONDED transaction
     * must NOT overwrite existing timestamps.
     */
    @Test
    void lateResponseAfterResponded_doesNotOverwriteTimestamps() {
        PostgresTrafficWriteRepository repo = new PostgresTrafficWriteRepository(jdbc);

        repo.upsert(requestPartial());
        repo.upsert(responsePartial());

        Long originalLatency = jdbc.queryForObject(
                "select latency_ms from traffic_transactions where correlation_id='corr-1'", Long.class);

        // Late duplicate response with a different timestamp
        Instant lateResponseTs = Instant.parse("2099-12-31T23:59:59Z");
        TrafficTransaction lateResponse = new TrafficTransaction(
                "corr-1", "tx-1", "ReqAuthPay", null, null, null, null,
                "infosrv-LATE", "Code=999", TrafficStatus.PENDING, null, lateResponseTs, null,
                "local", null, "<late-ans/>", now, now, false, null);
        repo.upsert(lateResponse);

        assertThat(jdbc.queryForObject("select latency_ms from traffic_transactions where correlation_id='corr-1'", Long.class))
                .as("latency must not be recomputed from late response timestamp")
                .isEqualTo(originalLatency);
        assertThat(jdbc.queryForObject("select upstream from traffic_transactions where correlation_id='corr-1'", String.class))
                .as("upstream must not be overwritten by late event")
                .isEqualTo("infosrv");
        assertThat(jdbc.queryForObject("select outcome from traffic_transactions where correlation_id='corr-1'", String.class))
                .as("outcome must not be overwritten by late event")
                .isEqualTo("Code=0");
    }
}
