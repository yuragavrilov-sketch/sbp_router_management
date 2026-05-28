package ru.copperside.sbprouter.management.routingconfig.adapter.out.postgres;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.copperside.sbprouter.management.routingconfig.application.port.out.RoutingFlagRepository;
import ru.copperside.sbprouter.management.routingconfig.domain.ConfigStatus;
import ru.copperside.sbprouter.management.routingconfig.domain.RoutingFlag;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class PostgresRoutingFlagRepository implements RoutingFlagRepository {

    private final JdbcTemplate jdbc;

    public PostgresRoutingFlagRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public RoutingFlag save(RoutingFlag f) {
        jdbc.update("""
                insert into routing_flags
                    (id, key, value, status, removal, created_at, updated_at)
                values (?, ?, ?, ?, ?, ?, ?)
                on conflict (id) do update set
                    key = excluded.key,
                    value = excluded.value,
                    status = excluded.status,
                    removal = excluded.removal,
                    updated_at = excluded.updated_at
                """,
                f.id(), f.key(), f.value(), f.status().name(), f.removal(),
                Timestamp.from(f.createdAt()), Timestamp.from(f.updatedAt()));
        return f;
    }

    @Override
    public Optional<RoutingFlag> findWorkingByKey(String key) {
        return jdbc.query("""
                select * from routing_flags
                where key = ? and status in ('DRAFT', 'ACTIVE')
                order by case status when 'DRAFT' then 0 else 1 end, updated_at desc
                limit 1
                """, MAPPER, key).stream().findFirst();
    }

    @Override
    public List<RoutingFlag> findAll() {
        return jdbc.query("select * from routing_flags order by key", MAPPER);
    }

    private static final RowMapper<RoutingFlag> MAPPER = (ResultSet rs, int n) -> new RoutingFlag(
            rs.getObject("id", UUID.class),
            rs.getString("key"),
            rs.getString("value"),
            ConfigStatus.valueOf(rs.getString("status")),
            rs.getBoolean("removal"),
            rs.getTimestamp("created_at").toInstant(),
            rs.getTimestamp("updated_at").toInstant());
}
