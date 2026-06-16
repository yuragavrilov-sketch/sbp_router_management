package ru.copperside.sbprouter.management.routing.adapter.out.postgres;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.copperside.sbprouter.management.routing.application.port.out.RoutingConfigRepository;
import ru.copperside.sbprouter.management.routing.domain.RoutingConfig;

import java.util.List;
import java.util.Optional;

@Repository
public class PostgresRoutingConfigRepository implements RoutingConfigRepository {

    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper = new ObjectMapper();

    public PostgresRoutingConfigRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<RoutingConfig> latest() {
        List<RoutingConfig> rows = jdbc.query(
                "select payload from routing_config order by version desc limit 1",
                (rs, n) -> read(rs.getString("payload")));
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    @Override
    public long nextVersion() {
        Long max = jdbc.queryForObject("select coalesce(max(version), 0) from routing_config", Long.class);
        return (max == null ? 0L : max) + 1L;
    }

    @Override
    public void save(RoutingConfig config) {
        jdbc.update("insert into routing_config (version, payload) values (?, ?)",
                config.version(), write(config));
    }

    private RoutingConfig read(String json) {
        try {
            return mapper.readValue(json, RoutingConfig.class);
        } catch (Exception e) {
            throw new IllegalStateException("corrupt routing_config payload", e);
        }
    }

    private String write(RoutingConfig config) {
        try {
            return mapper.writeValueAsString(config);
        } catch (Exception e) {
            throw new IllegalStateException("cannot serialize routing_config", e);
        }
    }
}
