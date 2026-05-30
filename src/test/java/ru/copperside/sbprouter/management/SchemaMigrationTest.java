package ru.copperside.sbprouter.management;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import ru.copperside.sbprouter.management.support.PostgresTestSupport;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@DirtiesContext
class SchemaMigrationTest extends PostgresTestSupport {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void migratesAllTables() {
        Integer tables = jdbc.queryForObject(
                "select count(*) from information_schema.tables where table_schema = 'sbp_router_management'",
                Integer.class);
        assertThat(tables).isGreaterThanOrEqualTo(7);
    }

    @Test
    void enforcesSingleActiveTerminalConfig() {
        assertThat(jdbc.queryForObject(
                "select count(*) from pg_indexes where indexname = 'terminal_config_single_active_uk'",
                Integer.class)).isEqualTo(1);
    }

    @Test
    void dropsRoutingManifestChecksumUniqueConstraint() {
        assertThat(jdbc.queryForObject(
                "select count(*) from pg_constraint where conname = 'routing_manifests_checksum_uk'",
                Integer.class)).isZero();
    }

    @Test
    void migratesTrafficTransactionsTable() {
        Integer count = jdbc.queryForObject(
                "select count(*) from information_schema.tables " +
                        "where table_schema = 'sbp_router_management' and table_name = 'traffic_transactions'",
                Integer.class);
        assertThat(count).isEqualTo(1);
    }
}
