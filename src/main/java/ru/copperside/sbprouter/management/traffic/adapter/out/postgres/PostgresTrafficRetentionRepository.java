package ru.copperside.sbprouter.management.traffic.adapter.out.postgres;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.copperside.sbprouter.management.traffic.application.port.out.TrafficRetentionRepository;

import java.sql.Timestamp;
import java.time.Instant;

@Repository
public class PostgresTrafficRetentionRepository implements TrafficRetentionRepository {

    private final JdbcTemplate jdbc;

    public PostgresTrafficRetentionRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public int deleteOlderThan(Instant cutoff) {
        return jdbc.update("delete from traffic_transactions where created_at < ?", Timestamp.from(cutoff));
    }
}
