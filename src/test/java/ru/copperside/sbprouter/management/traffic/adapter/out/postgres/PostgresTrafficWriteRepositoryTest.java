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
class PostgresTrafficWriteRepositoryTest extends PostgresTestSupport {

    @Autowired
    JdbcTemplate jdbc;

    private final Instant now = Instant.parse("2026-05-29T09:00:05Z");

    private TrafficTransaction requestPartial() {
        return new TrafficTransaction("corr-1", "tx-1", "ReqAuthPay", "owner-A", "route-x",
                null, null, TrafficStatus.PENDING, Instant.parse("2026-05-29T09:00:00Z"), null, null,
                "local", "<req/>", null, now, now);
    }

    private TrafficTransaction responsePartial() {
        return new TrafficTransaction("corr-1", "tx-1", "ReqAuthPay", null, null,
                "infosrv", "Code=0", TrafficStatus.PENDING, null, Instant.parse("2026-05-29T09:00:00.040Z"), null,
                "local", null, "<ans/>", now, now);
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
}
