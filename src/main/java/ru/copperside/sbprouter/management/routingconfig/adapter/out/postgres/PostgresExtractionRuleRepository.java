package ru.copperside.sbprouter.management.routingconfig.adapter.out.postgres;

import org.postgresql.util.PGobject;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.copperside.sbprouter.management.routingconfig.application.port.out.ExtractionRuleRepository;
import ru.copperside.sbprouter.management.routingconfig.domain.ConfigStatus;
import ru.copperside.sbprouter.management.routingconfig.domain.ExtractionRule;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class PostgresExtractionRuleRepository implements ExtractionRuleRepository {

    private final JdbcTemplate jdbc;

    public PostgresExtractionRuleRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public ExtractionRule save(ExtractionRule r) {
        jdbc.update("""
                insert into extraction_rules
                    (id, message_type, routing_fields_json, extra_fields_json,
                     status, removal, version, created_at, updated_at)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                on conflict (id) do update set
                    message_type = excluded.message_type,
                    routing_fields_json = excluded.routing_fields_json,
                    extra_fields_json = excluded.extra_fields_json,
                    status = excluded.status,
                    removal = excluded.removal,
                    version = excluded.version,
                    updated_at = excluded.updated_at
                """,
                r.id(), r.messageType(),
                jsonb(RoutingConfigJson.write(r.routingFields())),
                jsonb(RoutingConfigJson.write(r.extraFields())),
                r.status().name(), r.removal(), r.version(),
                Timestamp.from(r.createdAt()), Timestamp.from(r.updatedAt()));
        return r;
    }

    @Override
    public Optional<ExtractionRule> findById(UUID id) {
        return jdbc.query("select * from extraction_rules where id = ?", MAPPER, id).stream().findFirst();
    }

    @Override
    public List<ExtractionRule> findAll() {
        return jdbc.query("select * from extraction_rules order by message_type", MAPPER);
    }

    private static PGobject jsonb(String json) {
        PGobject obj = new PGobject();
        obj.setType("jsonb");
        try {
            obj.setValue(json);
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
        return obj;
    }

    private static final RowMapper<ExtractionRule> MAPPER = (ResultSet rs, int n) -> new ExtractionRule(
            rs.getObject("id", UUID.class),
            rs.getString("message_type"),
            RoutingConfigJson.read(rs.getString("routing_fields_json")),
            RoutingConfigJson.read(rs.getString("extra_fields_json")),
            ConfigStatus.valueOf(rs.getString("status")),
            rs.getBoolean("removal"),
            rs.getInt("version"),
            rs.getTimestamp("created_at").toInstant(),
            rs.getTimestamp("updated_at").toInstant());
}
