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

import java.sql.Timestamp;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@DirtiesContext
class PostgresTrafficRetentionRepositoryTest extends PostgresTestSupport {

    @Autowired
    JdbcTemplate jdbc;

    private void insert(String id, Instant createdAt) {
        jdbc.update("insert into traffic_transactions (correlation_id, status, created_at, updated_at) values (?, 'PENDING', ?, ?)",
                id, Timestamp.from(createdAt), Timestamp.from(createdAt));
    }

    @Test
    void deletesOnlyRowsOlderThanCutoff() {
        insert("old", Instant.parse("2026-04-01T00:00:00Z"));
        insert("recent", Instant.parse("2026-05-28T00:00:00Z"));
        PostgresTrafficRetentionRepository repo = new PostgresTrafficRetentionRepository(jdbc);

        int deleted = repo.deleteOlderThan(Instant.parse("2026-05-01T00:00:00Z"));

        assertThat(deleted).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from traffic_transactions", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select correlation_id from traffic_transactions", String.class)).isEqualTo("recent");
    }
}
