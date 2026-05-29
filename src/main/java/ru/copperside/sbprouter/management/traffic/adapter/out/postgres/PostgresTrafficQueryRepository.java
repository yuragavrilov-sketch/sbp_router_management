package ru.copperside.sbprouter.management.traffic.adapter.out.postgres;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.copperside.sbprouter.management.traffic.application.TrafficListResult;
import ru.copperside.sbprouter.management.traffic.application.TrafficQuery;
import ru.copperside.sbprouter.management.traffic.application.port.out.TrafficQueryRepository;
import ru.copperside.sbprouter.management.traffic.domain.TrafficStats;
import ru.copperside.sbprouter.management.traffic.domain.TrafficStatus;
import ru.copperside.sbprouter.management.traffic.domain.TrafficTransaction;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class PostgresTrafficQueryRepository implements TrafficQueryRepository {

    private final JdbcTemplate jdbc;

    public PostgresTrafficQueryRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public TrafficListResult list(TrafficQuery query) {
        StringBuilder where = new StringBuilder(" where 1=1");
        List<Object> args = new ArrayList<>();
        appendFilters(query, where, args);

        Long total = jdbc.queryForObject("select count(*) from traffic_transactions" + where, Long.class, args.toArray());

        int size = query.safeSize();
        int offset = query.safePage() * size;
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(size);
        pageArgs.add(offset);
        List<TrafficTransaction> rows = jdbc.query(
                "select correlation_id, tx_id, request_type, terminal_owner, route, upstream, outcome, status, "
                        + "request_at, response_at, latency_ms, env, created_at, updated_at "
                        + "from traffic_transactions" + where + " order by created_at desc limit ? offset ?",
                LIST_MAPPER, pageArgs.toArray());
        return new TrafficListResult(rows, total == null ? 0 : total, query.safePage(), size);
    }

    @Override
    public Optional<TrafficTransaction> find(String correlationId) {
        return jdbc.query("select * from traffic_transactions where correlation_id = ?", FULL_MAPPER, correlationId)
                .stream().findFirst();
    }

    @Override
    public TrafficStats stats(Instant from, Instant to) {
        Object[] window = {Timestamp.from(from), Timestamp.from(to)};
        String w = " where created_at >= ? and created_at < ?";

        Map<String, Object> totals = jdbc.queryForMap(
                "select count(*) total, count(*) filter (where status='RESPONDED') responded, "
                        + "count(*) filter (where status='PENDING') pending from traffic_transactions" + w, window);

        Map<String, Long> byOutcome = groupCounts(
                "select coalesce(outcome,'(none)') k, count(*) c from traffic_transactions" + w + " group by 1", window);
        Map<String, Long> byRequestType = groupCounts(
                "select coalesce(request_type,'(none)') k, count(*) c from traffic_transactions" + w + " group by 1", window);
        Map<String, Long> byUpstream = groupCounts(
                "select coalesce(upstream,'(none)') k, count(*) c from traffic_transactions" + w + " group by 1", window);

        Map<String, Object> lat = jdbc.queryForMap(
                "select percentile_cont(0.95) within group (order by latency_ms) p95, "
                        + "percentile_cont(0.99) within group (order by latency_ms) p99, "
                        + "avg(latency_ms) avg from traffic_transactions" + w + " and latency_ms is not null", window);

        List<TrafficStats.Bucket> buckets = jdbc.query(
                "select date_trunc('minute', created_at) m, count(*) c from traffic_transactions" + w
                        + " group by 1 order by 1", (rs, n) ->
                        new TrafficStats.Bucket(rs.getTimestamp("m").toInstant(), rs.getLong("c")), window);

        return new TrafficStats(
                ((Number) totals.get("total")).longValue(),
                ((Number) totals.get("responded")).longValue(),
                ((Number) totals.get("pending")).longValue(),
                byOutcome, byRequestType, byUpstream,
                round(lat.get("p95")), round(lat.get("p99")), round(lat.get("avg")),
                buckets);
    }

    private void appendFilters(TrafficQuery q, StringBuilder where, List<Object> args) {
        if (q.requestType() != null) { where.append(" and request_type = ?"); args.add(q.requestType()); }
        if (q.terminalOwner() != null) { where.append(" and terminal_owner = ?"); args.add(q.terminalOwner()); }
        if (q.upstream() != null) { where.append(" and upstream = ?"); args.add(q.upstream()); }
        if (q.outcome() != null) { where.append(" and outcome = ?"); args.add(q.outcome()); }
        if (q.status() != null) { where.append(" and status = ?"); args.add(q.status()); }
        if (q.from() != null) { where.append(" and created_at >= ?"); args.add(Timestamp.from(q.from())); }
        if (q.to() != null) { where.append(" and created_at < ?"); args.add(Timestamp.from(q.to())); }
        if (q.q() != null && !q.q().isBlank()) {
            where.append(" and (correlation_id ilike ? or tx_id ilike ?)");
            args.add("%" + q.q() + "%");
            args.add("%" + q.q() + "%");
        }
    }

    private Map<String, Long> groupCounts(String sql, Object[] args) {
        Map<String, Long> map = new LinkedHashMap<>();
        jdbc.query(sql, rs -> { map.put(rs.getString("k"), rs.getLong("c")); }, args);
        return map;
    }

    private static Long round(Object numeric) {
        return numeric == null ? null : Math.round(((Number) numeric).doubleValue());
    }

    private static final RowMapper<TrafficTransaction> LIST_MAPPER = (rs, n) -> map(rs, false);
    private static final RowMapper<TrafficTransaction> FULL_MAPPER = (rs, n) -> map(rs, true);

    private static TrafficTransaction map(ResultSet rs, boolean withXml) throws SQLException {
        Long latency = (Long) rs.getObject("latency_ms");
        return new TrafficTransaction(
                rs.getString("correlation_id"),
                rs.getString("tx_id"),
                rs.getString("request_type"),
                rs.getString("terminal_owner"),
                rs.getString("route"),
                rs.getString("upstream"),
                rs.getString("outcome"),
                TrafficStatus.valueOf(rs.getString("status")),
                instant(rs.getTimestamp("request_at")),
                instant(rs.getTimestamp("response_at")),
                latency,
                rs.getString("env"),
                withXml ? rs.getString("request_xml") : null,
                withXml ? rs.getString("response_xml") : null,
                instant(rs.getTimestamp("created_at")),
                instant(rs.getTimestamp("updated_at")));
    }

    private static Instant instant(Timestamp ts) {
        return ts == null ? null : ts.toInstant();
    }
}
