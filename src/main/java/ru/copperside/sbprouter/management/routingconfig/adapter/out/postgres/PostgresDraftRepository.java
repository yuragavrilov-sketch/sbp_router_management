package ru.copperside.sbprouter.management.routingconfig.adapter.out.postgres;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.copperside.sbprouter.management.routingconfig.application.port.out.DraftRepository;

import java.util.List;

@Repository
public class PostgresDraftRepository implements DraftRepository {

    private static final List<String> TABLES = List.of(
            "upstreams", "extraction_rules", "terminal_routing_config", "tkb_pay_list_entries", "routing_flags");

    private final JdbcTemplate jdbc;

    public PostgresDraftRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public void discardAll() {
        for (String table : TABLES) {
            jdbc.update("delete from " + table + " where status = 'DRAFT'");
            jdbc.update("update " + table + " set removal = false where removal = true and status = 'ACTIVE'");
        }
    }
}
