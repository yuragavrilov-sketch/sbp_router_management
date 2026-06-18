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

/**
 * IT: verifies that a response event with a {@code <fault>} element results in
 * {@code RESPONDED_WITH_ERROR} status and the fault_string is persisted.
 * A normal response must still yield {@code RESPONDED} with null fault_string.
 */
@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@DirtiesContext
class PostgresTrafficFaultIT extends PostgresTestSupport {

    @Autowired
    JdbcTemplate jdbc;

    private final Instant t0 = Instant.parse("2026-06-17T10:00:00Z");
    private final Instant t1 = Instant.parse("2026-06-17T10:00:00.050Z");
    private final Instant now = Instant.parse("2026-06-17T10:00:05Z");

    private static final String FAULT_RESPONSE_XML =
            "<Document><GCSvc><fault>" +
            "<faultcode>9999</faultcode>" +
            "<faultstring>DBMS error: ORA-1555</faultstring>" +
            "</fault></GCSvc></Document>";

    private static final String NORMAL_RESPONSE_XML =
            "<Document><GCSvc><AnsAuthPay><rc>0</rc></AnsAuthPay></GCSvc></Document>";

    @Test
    void faultResponseProducesRespondedWithErrorAndStoresFaultString() {
        PostgresTrafficWriteRepository repo = new PostgresTrafficWriteRepository(jdbc);

        // request
        repo.upsert(new TrafficTransaction("fault-corr-1", "fault-tx-1", "ReqAuthPay", null, null,
                "owner-A", "route-x", null, null,
                TrafficStatus.PENDING, t0, null, null, "local",
                "<req/>", null, now, now, false, null));

        // response with fault
        repo.upsert(new TrafficTransaction("fault-corr-1", "fault-tx-1", "ReqAuthPay", null, null,
                null, null, "infosrv", "Code=9999",
                TrafficStatus.PENDING, null, t1, null, "local",
                null, FAULT_RESPONSE_XML, now, now, true, "DBMS error: ORA-1555"));

        assertThat(jdbc.queryForObject(
                "select status from traffic_transactions where correlation_id='fault-corr-1'", String.class))
                .isEqualTo("RESPONDED_WITH_ERROR");
        assertThat(jdbc.queryForObject(
                "select fault_string from traffic_transactions where correlation_id='fault-corr-1'", String.class))
                .isEqualTo("DBMS error: ORA-1555");
        assertThat(jdbc.queryForObject(
                "select latency_ms from traffic_transactions where correlation_id='fault-corr-1'", Long.class))
                .isEqualTo(50L);
    }

    @Test
    void normalResponseProducesRespondedAndNullFaultString() {
        PostgresTrafficWriteRepository repo = new PostgresTrafficWriteRepository(jdbc);

        repo.upsert(new TrafficTransaction("normal-corr-1", "normal-tx-1", "ReqAuthPay", null, null,
                "owner-B", "route-y", null, null,
                TrafficStatus.PENDING, t0, null, null, "local",
                "<req/>", null, now, now, false, null));

        repo.upsert(new TrafficTransaction("normal-corr-1", "normal-tx-1", "ReqAuthPay", null, null,
                null, null, "infosrv", "Code=0",
                TrafficStatus.PENDING, null, t1, null, "local",
                null, NORMAL_RESPONSE_XML, now, now, false, null));

        assertThat(jdbc.queryForObject(
                "select status from traffic_transactions where correlation_id='normal-corr-1'", String.class))
                .isEqualTo("RESPONDED");
        assertThat(jdbc.queryForObject(
                "select fault_string from traffic_transactions where correlation_id='normal-corr-1'", String.class))
                .isNull();
    }

    @Test
    void faultResponseArrivingBeforeRequestIsCorrectlyMerged() {
        PostgresTrafficWriteRepository repo = new PostgresTrafficWriteRepository(jdbc);

        // response arrives first
        repo.upsert(new TrafficTransaction("fault-corr-2", "fault-tx-2", "ReqAuthPay", null, null,
                null, null, "infosrv", "Code=9999",
                TrafficStatus.PENDING, null, t1, null, "local",
                null, FAULT_RESPONSE_XML, now, now, true, "DBMS error: ORA-1555"));

        // then request
        repo.upsert(new TrafficTransaction("fault-corr-2", "fault-tx-2", "ReqAuthPay", null, null,
                "owner-A", "route-x", null, null,
                TrafficStatus.PENDING, t0, null, null, "local",
                "<req/>", null, now, now, false, null));

        assertThat(jdbc.queryForObject(
                "select status from traffic_transactions where correlation_id='fault-corr-2'", String.class))
                .isEqualTo("RESPONDED_WITH_ERROR");
        assertThat(jdbc.queryForObject(
                "select fault_string from traffic_transactions where correlation_id='fault-corr-2'", String.class))
                .isEqualTo("DBMS error: ORA-1555");
    }
}
