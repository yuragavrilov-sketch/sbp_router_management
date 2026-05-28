package ru.copperside.sbprouter.management.routingmanifest.application;

import ru.copperside.sbprouter.management.routingconfig.domain.ConfigStatus;
import ru.copperside.sbprouter.management.routingconfig.domain.ExtractionRule;
import ru.copperside.sbprouter.management.routingconfig.domain.RoutingFlag;
import ru.copperside.sbprouter.management.routingconfig.domain.TerminalRoutingConfig;
import ru.copperside.sbprouter.management.routingconfig.domain.TkbPayListEntry;
import ru.copperside.sbprouter.management.routingconfig.domain.Upstream;
import ru.copperside.sbprouter.management.routingmanifest.domain.PendingChanges;
import ru.copperside.sbprouter.management.routingmanifest.domain.ProspectiveConfig;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

public class ProspectiveConfigAssembler {

    public ProspectiveConfig assemble(
            List<Upstream> upstreams,
            List<ExtractionRule> extractionRules,
            List<TerminalRoutingConfig> terminalConfigs,
            List<TkbPayListEntry> tkbPayList,
            List<RoutingFlag> routingFlags) {
        return new ProspectiveConfig(
                resolveUpstreams(upstreams),
                resolve(extractionRules, ExtractionRule::messageType, ExtractionRule::status, ExtractionRule::removal),
                resolveTerminal(terminalConfigs),
                resolve(tkbPayList, TkbPayListEntry::rcvTspId, TkbPayListEntry::status, TkbPayListEntry::removal),
                resolve(routingFlags, RoutingFlag::key, RoutingFlag::status, RoutingFlag::removal));
    }

    public List<Upstream> resolveUpstreams(List<Upstream> rows) {
        return resolve(rows, Upstream::name, Upstream::status, Upstream::removal);
    }

    public TerminalRoutingConfig resolveTerminal(List<TerminalRoutingConfig> rows) {
        if (rows == null) {
            return null;
        }
        TerminalRoutingConfig chosen = null;
        for (TerminalRoutingConfig row : rows) {
            if (row.status() == ConfigStatus.ACTIVE && !row.removal()) {
                chosen = row;
            }
        }
        for (TerminalRoutingConfig row : rows) {
            if (row.status() == ConfigStatus.DRAFT && !row.removal()) {
                chosen = row;
            }
        }
        for (TerminalRoutingConfig row : rows) {
            if (row.removal()) {
                chosen = null;
            }
        }
        return chosen;
    }

    private <T> List<T> resolve(List<T> rows, Function<T, String> key,
                                Function<T, ConfigStatus> status, Function<T, Boolean> removal) {
        Map<String, T> chosen = new LinkedHashMap<>();
        rows.stream().filter(r -> status.apply(r) == ConfigStatus.ACTIVE && !removal.apply(r))
                .forEach(r -> chosen.put(key.apply(r), r));
        rows.stream().filter(r -> status.apply(r) == ConfigStatus.DRAFT && !removal.apply(r))
                .forEach(r -> chosen.put(key.apply(r), r));
        rows.stream().filter(removal::apply)
                .forEach(r -> chosen.remove(key.apply(r)));
        return new ArrayList<>(chosen.values());
    }

    public PendingChanges pendingChanges(
            List<Upstream> upstreams,
            List<ExtractionRule> extractionRules,
            List<TerminalRoutingConfig> terminalConfigs,
            List<TkbPayListEntry> tkbPayList,
            List<RoutingFlag> routingFlags,
            int nextVersion,
            Integer currentVersion) {
        List<PendingChanges.Entry> entries = new ArrayList<>();
        diff(upstreams, Upstream::name, Upstream::id, Upstream::status, Upstream::removal, "UPSTREAM", entries);
        diff(extractionRules, ExtractionRule::messageType, ExtractionRule::id, ExtractionRule::status,
                ExtractionRule::removal, "EXTRACTION_RULE", entries);
        diff(tkbPayList, TkbPayListEntry::rcvTspId, TkbPayListEntry::id, TkbPayListEntry::status,
                TkbPayListEntry::removal, "TKB_PAY_ENTRY", entries);
        diff(routingFlags, RoutingFlag::key, RoutingFlag::id, RoutingFlag::status, RoutingFlag::removal,
                "ROUTING_FLAG", entries);
        diffTerminal(terminalConfigs, entries);
        return new PendingChanges(entries.size(), currentVersion, nextVersion, entries);
    }

    private <T> void diff(List<T> rows, Function<T, String> key, Function<T, UUID> id,
                          Function<T, ConfigStatus> status, Function<T, Boolean> removal,
                          String kind, List<PendingChanges.Entry> entries) {
        // live = all rows currently ACTIVE (regardless of removal flag — removal is a pending intent)
        Map<String, UUID> live = new LinkedHashMap<>();
        rows.stream().filter(r -> status.apply(r) == ConfigStatus.ACTIVE)
                .forEach(r -> live.put(key.apply(r), id.apply(r)));
        // prospective = what will be ACTIVE after publish
        Map<String, UUID> prospective = new LinkedHashMap<>();
        rows.stream().filter(r -> status.apply(r) == ConfigStatus.ACTIVE && !removal.apply(r))
                .forEach(r -> prospective.put(key.apply(r), id.apply(r)));
        rows.stream().filter(r -> status.apply(r) == ConfigStatus.DRAFT && !removal.apply(r))
                .forEach(r -> prospective.put(key.apply(r), id.apply(r)));
        rows.stream().filter(removal::apply).forEach(r -> prospective.remove(key.apply(r)));

        for (Map.Entry<String, UUID> p : prospective.entrySet()) {
            if (!live.containsKey(p.getKey())) {
                entries.add(new PendingChanges.Entry(kind, "ADDED", p.getKey()));
            } else if (!live.get(p.getKey()).equals(p.getValue())) {
                entries.add(new PendingChanges.Entry(kind, "MODIFIED", p.getKey()));
            }
        }
        for (String liveKey : live.keySet()) {
            if (!prospective.containsKey(liveKey)) {
                entries.add(new PendingChanges.Entry(kind, "REMOVED", liveKey));
            }
        }
    }

    private void diffTerminal(List<TerminalRoutingConfig> rows, List<PendingChanges.Entry> entries) {
        if (rows == null) {
            return;
        }
        // live = the currently ACTIVE row (regardless of removal flag)
        TerminalRoutingConfig live = rows.stream()
                .filter(r -> r.status() == ConfigStatus.ACTIVE).reduce((a, b) -> b).orElse(null);
        TerminalRoutingConfig prospective = resolveTerminal(rows);
        UUID liveId = live == null ? null : live.id();
        UUID prospId = prospective == null ? null : prospective.id();
        if (liveId == null && prospId != null) {
            entries.add(new PendingChanges.Entry("TERMINAL_CONFIG", "ADDED", "terminal-config"));
        } else if (liveId != null && prospId == null) {
            entries.add(new PendingChanges.Entry("TERMINAL_CONFIG", "REMOVED", "terminal-config"));
        } else if (liveId != null && !liveId.equals(prospId)) {
            entries.add(new PendingChanges.Entry("TERMINAL_CONFIG", "MODIFIED", "terminal-config"));
        }
    }
}
