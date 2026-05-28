package ru.copperside.sbprouter.management.routingmanifest.adapter.out.postgres;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.copperside.sbprouter.management.routingconfig.domain.ExtractionRule;
import ru.copperside.sbprouter.management.routingconfig.domain.RoutingFlag;
import ru.copperside.sbprouter.management.routingconfig.domain.TkbPayListEntry;
import ru.copperside.sbprouter.management.routingconfig.domain.Upstream;
import ru.copperside.sbprouter.management.routingmanifest.application.port.out.RoutingManifestRepository;
import ru.copperside.sbprouter.management.routingmanifest.domain.ProspectiveConfig;
import ru.copperside.sbprouter.management.routingmanifest.domain.RoutingManifest;
import ru.copperside.sbprouter.management.routingmanifest.domain.RoutingManifestPayload;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class PostgresRoutingManifestRepository implements RoutingManifestRepository {

    private final JdbcTemplate jdbc;

    public PostgresRoutingManifestRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public int nextVersion() {
        Integer max = jdbc.queryForObject("select coalesce(max(version), 0) from routing_manifests", Integer.class);
        return (max == null ? 0 : max) + 1;
    }

    @Override
    public Optional<Integer> latestVersion() {
        Integer max = jdbc.queryForObject("select max(version) from routing_manifests", Integer.class);
        return Optional.ofNullable(max);
    }

    @Override
    @Transactional
    public RoutingManifest publish(RoutingManifest manifest, ProspectiveConfig prospective) {
        jdbc.update("""
                insert into routing_manifests (id, version, status, checksum, payload_json, created_at)
                values (?, ?, ?, ?, ?, ?)
                """,
                manifest.id(), manifest.version(), manifest.status().name(), manifest.checksum(),
                ManifestJson.jsonb(manifest.payload()), Timestamp.from(manifest.createdAt()));

        int position = 0;
        position = insertEntities(manifest.id(), position, "UPSTREAM",
                prospective.upstreams().stream().map(Upstream::id).toList(),
                prospective.upstreams().stream().map(Upstream::version).toList());
        position = insertEntities(manifest.id(), position, "EXTRACTION_RULE",
                prospective.extractionRules().stream().map(ExtractionRule::id).toList(),
                prospective.extractionRules().stream().map(ExtractionRule::version).toList());
        if (prospective.terminalConfig() != null) {
            position = insertEntities(manifest.id(), position, "TERMINAL_CONFIG",
                    List.of(prospective.terminalConfig().id()), List.of(prospective.terminalConfig().version()));
        }
        position = insertEntities(manifest.id(), position, "TKB_PAY_ENTRY",
                prospective.tkbPayList().stream().map(TkbPayListEntry::id).toList(),
                prospective.tkbPayList().stream().map(e -> 1).toList());
        insertEntities(manifest.id(), position, "ROUTING_FLAG",
                prospective.routingFlags().stream().map(RoutingFlag::id).toList(),
                prospective.routingFlags().stream().map(f -> 1).toList());

        promote("upstreams", prospective.upstreams().stream().map(Upstream::id).toList());
        promote("extraction_rules", prospective.extractionRules().stream().map(ExtractionRule::id).toList());
        promote("terminal_routing_config",
                prospective.terminalConfig() == null ? List.of() : List.of(prospective.terminalConfig().id()));
        promote("tkb_pay_list_entries", prospective.tkbPayList().stream().map(TkbPayListEntry::id).toList());
        promote("routing_flags", prospective.routingFlags().stream().map(RoutingFlag::id).toList());

        return manifest;
    }

    private int insertEntities(UUID manifestId, int startPosition, String kind,
                               List<UUID> ids, List<Integer> versions) {
        int position = startPosition;
        for (int i = 0; i < ids.size(); i++) {
            jdbc.update("""
                    insert into routing_manifest_entities
                        (manifest_id, entity_kind, entity_id, entity_version, position, payload_json)
                    values (?, ?, ?, ?, ?, ?)
                    """,
                    manifestId, kind, ids.get(i), versions.get(i), position,
                    ManifestJson.jsonb(Map.of("entityId", ids.get(i).toString())));
            position++;
        }
        return position;
    }

    /**
     * Archive all active rows, activate the chosen ids, archive any leftover drafts.
     * Order: archive-active first → then activate chosen → then archive remaining drafts.
     * This satisfies partial unique indexes (e.g. terminal_config_single_active_uk) which
     * allow only one ACTIVE row at a time: the old ACTIVE must be archived before the new
     * one is activated.
     */
    private void promote(String table, List<UUID> activateIds) {
        jdbc.update("update " + table + " set status = 'ARCHIVED' where status = 'ACTIVE'");
        for (UUID id : activateIds) {
            jdbc.update("update " + table + " set status = 'ACTIVE', removal = false where id = ?", id);
        }
        jdbc.update("update " + table + " set status = 'ARCHIVED' where status = 'DRAFT'");
    }

    @Override
    public Optional<RoutingManifest> findLatest() {
        return jdbc.query("select * from routing_manifests order by version desc limit 1", MAPPER)
                .stream().findFirst();
    }

    @Override
    public Optional<RoutingManifest> find(UUID id) {
        return jdbc.query("select * from routing_manifests where id = ?", MAPPER, id).stream().findFirst();
    }

    private static final RowMapper<RoutingManifest> MAPPER = PostgresRoutingManifestRepository::mapRow;

    private static RoutingManifest mapRow(ResultSet rs, int rowNum) throws SQLException {
        RoutingManifestPayload payload = ManifestJson.readPayload(rs.getString("payload_json"));
        return new RoutingManifest(
                rs.getObject("id", UUID.class),
                rs.getInt("version"),
                payload.status(),
                rs.getString("checksum"),
                rs.getTimestamp("created_at").toInstant(),
                payload.content(),
                payload);
    }
}
