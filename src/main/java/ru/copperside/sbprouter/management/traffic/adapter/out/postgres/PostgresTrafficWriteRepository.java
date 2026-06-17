package ru.copperside.sbprouter.management.traffic.adapter.out.postgres;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.copperside.sbprouter.management.traffic.application.port.out.TrafficWriteRepository;
import ru.copperside.sbprouter.management.traffic.domain.TrafficTransaction;

import java.sql.Timestamp;

@Repository
public class PostgresTrafficWriteRepository implements TrafficWriteRepository {

    private final JdbcTemplate jdbc;

    public PostgresTrafficWriteRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void upsert(TrafficTransaction t) {
        jdbc.update("""
                insert into traffic_transactions as tt
                    (correlation_id, tx_id, request_type, operation_id, operation_type,
                     terminal_owner, route, upstream, outcome,
                     status, request_at, response_at, latency_ms, env, request_xml, response_xml,
                     created_at, updated_at)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?,
                        case when ?::timestamptz is not null then 'RESPONDED' else 'PENDING' end,
                        ?, ?, null, ?, ?, ?, ?, ?)
                on conflict (correlation_id) do update set
                    tx_id = coalesce(excluded.tx_id, tt.tx_id),
                    request_type = coalesce(excluded.request_type, tt.request_type),
                    operation_id = coalesce(tt.operation_id, excluded.operation_id),
                    operation_type = coalesce(tt.operation_type, excluded.operation_type),
                    terminal_owner = coalesce(excluded.terminal_owner, tt.terminal_owner),
                    route = coalesce(excluded.route, tt.route),
                    upstream = coalesce(excluded.upstream, tt.upstream),
                    outcome = coalesce(excluded.outcome, tt.outcome),
                    request_at = coalesce(excluded.request_at, tt.request_at),
                    response_at = coalesce(excluded.response_at, tt.response_at),
                    env = coalesce(excluded.env, tt.env),
                    request_xml = coalesce(excluded.request_xml, tt.request_xml),
                    response_xml = coalesce(excluded.response_xml, tt.response_xml),
                    updated_at = excluded.updated_at,
                    status = case when coalesce(excluded.response_at, tt.response_at) is not null
                                  then 'RESPONDED' else 'PENDING' end,
                    latency_ms = case
                        when coalesce(excluded.request_at, tt.request_at) is not null
                         and coalesce(excluded.response_at, tt.response_at) is not null
                        then (extract(epoch from (coalesce(excluded.response_at, tt.response_at)
                                                  - coalesce(excluded.request_at, tt.request_at))) * 1000)::bigint
                        else null end
                where not (tt.status = 'RESPONDED'
                       and tt.request_at  is not null
                       and tt.response_at is not null)
                """,
                t.correlationId(), t.txId(), t.requestType(), t.operationId(), t.operationType(),
                t.terminalOwner(), t.route(),
                t.upstream(), t.outcome(),
                ts(t.responseAt()),
                ts(t.requestAt()), ts(t.responseAt()), t.env(), t.requestXml(), t.responseXml(),
                ts(t.createdAt()), ts(t.updatedAt()));
    }

    private static Timestamp ts(java.time.Instant i) {
        return i == null ? null : Timestamp.from(i);
    }
}
