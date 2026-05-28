package ru.copperside.sbprouter.management.routingconfig.adapter.out.postgres;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.copperside.sbprouter.management.routingconfig.application.port.out.UpstreamRepository;
import ru.copperside.sbprouter.management.routingconfig.domain.ConfigStatus;
import ru.copperside.sbprouter.management.routingconfig.domain.Upstream;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class PostgresUpstreamRepository implements UpstreamRepository {

    private final JdbcTemplate jdbc;

    public PostgresUpstreamRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Upstream save(Upstream u) {
        jdbc.update("""
                insert into upstreams
                    (id, name, url, timeout_ms, retry_max_attempts, retry_backoff_ms,
                     status, removal, version, created_at, updated_at)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                on conflict (id) do update set
                    name = excluded.name,
                    url = excluded.url,
                    timeout_ms = excluded.timeout_ms,
                    retry_max_attempts = excluded.retry_max_attempts,
                    retry_backoff_ms = excluded.retry_backoff_ms,
                    status = excluded.status,
                    removal = excluded.removal,
                    version = excluded.version,
                    updated_at = excluded.updated_at
                """,
                u.id(), u.name(), u.url(), u.timeoutMs(), u.retryMaxAttempts(), u.retryBackoffMs(),
                u.status().name(), u.removal(), u.version(),
                Timestamp.from(u.createdAt()), Timestamp.from(u.updatedAt()));
        return u;
    }

    @Override
    public Optional<Upstream> findById(UUID id) {
        List<Upstream> rows = jdbc.query(
                "select * from upstreams where id = ?", MAPPER, id);
        return rows.stream().findFirst();
    }

    @Override
    public List<Upstream> findAll() {
        return jdbc.query("select * from upstreams order by name", MAPPER);
    }

    private static final RowMapper<Upstream> MAPPER = PostgresUpstreamRepository::mapRow;

    private static Upstream mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Upstream(
                rs.getObject("id", UUID.class),
                rs.getString("name"),
                rs.getString("url"),
                (Integer) rs.getObject("timeout_ms"),
                (Integer) rs.getObject("retry_max_attempts"),
                (Integer) rs.getObject("retry_backoff_ms"),
                ConfigStatus.valueOf(rs.getString("status")),
                rs.getBoolean("removal"),
                rs.getInt("version"),
                toInstant(rs.getTimestamp("created_at")),
                toInstant(rs.getTimestamp("updated_at")));
    }

    private static Instant toInstant(Timestamp ts) {
        return ts == null ? null : ts.toInstant();
    }
}
