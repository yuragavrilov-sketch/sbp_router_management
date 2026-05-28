package ru.copperside.sbprouter.management.routingconfig.adapter.out.postgres;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.copperside.sbprouter.management.routingconfig.application.port.out.TerminalRoutingConfigRepository;
import ru.copperside.sbprouter.management.routingconfig.domain.ConfigStatus;
import ru.copperside.sbprouter.management.routingconfig.domain.TerminalRoutingConfig;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class PostgresTerminalRoutingConfigRepository implements TerminalRoutingConfigRepository {

    private final JdbcTemplate jdbc;

    public PostgresTerminalRoutingConfigRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public TerminalRoutingConfig save(TerminalRoutingConfig c) {
        jdbc.update("""
                insert into terminal_routing_config
                    (id, c2b_field_name, b2c_field_name, tkb_pay_prefix,
                     status, removal, version, created_at, updated_at)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                on conflict (id) do update set
                    c2b_field_name = excluded.c2b_field_name,
                    b2c_field_name = excluded.b2c_field_name,
                    tkb_pay_prefix = excluded.tkb_pay_prefix,
                    status = excluded.status,
                    removal = excluded.removal,
                    version = excluded.version,
                    updated_at = excluded.updated_at
                """,
                c.id(), c.c2bFieldName(), c.b2cFieldName(), c.tkbPayPrefix(),
                c.status().name(), c.removal(), c.version(),
                Timestamp.from(c.createdAt()), Timestamp.from(c.updatedAt()));
        return c;
    }

    @Override
    public Optional<TerminalRoutingConfig> findWorking() {
        List<TerminalRoutingConfig> rows = jdbc.query("""
                select * from terminal_routing_config
                where status in ('DRAFT', 'ACTIVE')
                order by case status when 'DRAFT' then 0 else 1 end, updated_at desc
                limit 1
                """, MAPPER);
        return rows.stream().findFirst();
    }

    private static final RowMapper<TerminalRoutingConfig> MAPPER = (ResultSet rs, int n) -> new TerminalRoutingConfig(
            rs.getObject("id", UUID.class),
            rs.getString("c2b_field_name"),
            rs.getString("b2c_field_name"),
            rs.getString("tkb_pay_prefix"),
            ConfigStatus.valueOf(rs.getString("status")),
            rs.getBoolean("removal"),
            rs.getInt("version"),
            rs.getTimestamp("created_at").toInstant(),
            rs.getTimestamp("updated_at").toInstant());
}
