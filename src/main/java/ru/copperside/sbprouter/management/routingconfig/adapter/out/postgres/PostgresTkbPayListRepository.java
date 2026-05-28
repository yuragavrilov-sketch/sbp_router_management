package ru.copperside.sbprouter.management.routingconfig.adapter.out.postgres;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.copperside.sbprouter.management.routingconfig.application.port.out.TkbPayListRepository;
import ru.copperside.sbprouter.management.routingconfig.domain.ConfigStatus;
import ru.copperside.sbprouter.management.routingconfig.domain.TkbPayListEntry;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class PostgresTkbPayListRepository implements TkbPayListRepository {

    private final JdbcTemplate jdbc;

    public PostgresTkbPayListRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public TkbPayListEntry save(TkbPayListEntry e) {
        jdbc.update("""
                insert into tkb_pay_list_entries
                    (id, rcv_tsp_id, status, removal, created_at, updated_at)
                values (?, ?, ?, ?, ?, ?)
                on conflict (id) do update set
                    rcv_tsp_id = excluded.rcv_tsp_id,
                    status = excluded.status,
                    removal = excluded.removal,
                    updated_at = excluded.updated_at
                """,
                e.id(), e.rcvTspId(), e.status().name(), e.removal(),
                Timestamp.from(e.createdAt()), Timestamp.from(e.updatedAt()));
        return e;
    }

    @Override
    public Optional<TkbPayListEntry> findById(UUID id) {
        return jdbc.query("select * from tkb_pay_list_entries where id = ?", MAPPER, id).stream().findFirst();
    }

    @Override
    public List<TkbPayListEntry> findAll() {
        return jdbc.query("select * from tkb_pay_list_entries order by rcv_tsp_id", MAPPER);
    }

    private static final RowMapper<TkbPayListEntry> MAPPER = (ResultSet rs, int n) -> new TkbPayListEntry(
            rs.getObject("id", UUID.class),
            rs.getString("rcv_tsp_id"),
            ConfigStatus.valueOf(rs.getString("status")),
            rs.getBoolean("removal"),
            rs.getTimestamp("created_at").toInstant(),
            rs.getTimestamp("updated_at").toInstant());
}
