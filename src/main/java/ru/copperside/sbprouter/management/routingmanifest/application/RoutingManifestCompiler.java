package ru.copperside.sbprouter.management.routingmanifest.application;

import ru.copperside.sbprouter.management.routingconfig.domain.ExtractionRule;
import ru.copperside.sbprouter.management.routingconfig.domain.FieldBinding;
import ru.copperside.sbprouter.management.routingconfig.domain.RoutingFlag;
import ru.copperside.sbprouter.management.routingconfig.domain.TerminalRoutingConfig;
import ru.copperside.sbprouter.management.routingconfig.domain.TkbPayListEntry;
import ru.copperside.sbprouter.management.routingconfig.domain.Upstream;
import ru.copperside.sbprouter.management.routingmanifest.domain.ManifestDiagnostic;
import ru.copperside.sbprouter.management.routingmanifest.domain.ManifestDiagnosticSeverity;
import ru.copperside.sbprouter.management.routingmanifest.domain.ProspectiveConfig;
import ru.copperside.sbprouter.management.routingmanifest.domain.RoutingManifest;
import ru.copperside.sbprouter.management.routingmanifest.domain.RoutingManifestContent;
import ru.copperside.sbprouter.management.routingmanifest.domain.RoutingManifestContent.ExtractionRulePayload;
import ru.copperside.sbprouter.management.routingmanifest.domain.RoutingManifestContent.RetryPayload;
import ru.copperside.sbprouter.management.routingmanifest.domain.RoutingManifestContent.TerminalsPayload;
import ru.copperside.sbprouter.management.routingmanifest.domain.RoutingManifestContent.UpstreamPayload;
import ru.copperside.sbprouter.management.routingmanifest.domain.RoutingManifestPayload;
import ru.copperside.sbprouter.management.routingmanifest.domain.RoutingManifestProblemException;
import ru.copperside.sbprouter.management.routingmanifest.domain.RoutingManifestStatus;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

public class RoutingManifestCompiler {

    private final Clock clock;
    private final RoutingManifestCanonicalJson canonicalJson;

    public RoutingManifestCompiler(Clock clock) {
        this(clock, new RoutingManifestCanonicalJson());
    }

    RoutingManifestCompiler(Clock clock, RoutingManifestCanonicalJson canonicalJson) {
        this.clock = clock;
        this.canonicalJson = canonicalJson;
    }

    public RoutingManifest compile(int version, ProspectiveConfig config) {
        List<ManifestDiagnostic> diagnostics = new ArrayList<>();

        validateUpstreams(config.upstreams(), diagnostics);
        Set<String> routingFieldNames = validateExtractionRules(config.extractionRules(), diagnostics);
        validateTkbPayList(config.tkbPayList(), diagnostics);
        validateTerminal(config.terminalConfig(), routingFieldNames, diagnostics);

        if (!diagnostics.isEmpty()) {
            throw new RoutingManifestProblemException(
                    "ROUTING_MANIFEST_CONFLICT",
                    "Active routing configuration cannot be published",
                    diagnostics);
        }

        RoutingManifestContent content = buildContent(config);
        Instant createdAt = Instant.now(clock);
        RoutingManifestPayload payload = new RoutingManifestPayload(
                version, RoutingManifestStatus.VALID, createdAt, content, List.of());
        return new RoutingManifest(
                UUID.randomUUID(), version, RoutingManifestStatus.VALID,
                canonicalJson.checksum(content), createdAt, content, payload);
    }

    private void validateUpstreams(List<Upstream> upstreams, List<ManifestDiagnostic> diagnostics) {
        if (upstreams.isEmpty()) {
            diagnostics.add(diagnostic("MANIFEST_EMPTY_UPSTREAMS", "No active upstreams", List.of(), "upstreams"));
        }
        Set<String> names = new HashSet<>();
        for (Upstream u : upstreams) {
            if (!names.add(u.name())) {
                diagnostics.add(diagnostic("MANIFEST_DUPLICATE_UPSTREAM_NAME",
                        "Duplicate upstream name " + u.name(), List.of(u.id()), "upstreams"));
            }
            if (u.url() == null || !isAbsoluteUrl(u.url())) {
                diagnostics.add(diagnostic("MANIFEST_UPSTREAM_URL_INVALID",
                        "Upstream url is not a valid absolute URL", List.of(u.id()), "upstreams." + u.name()));
            }
        }
    }

    private Set<String> validateExtractionRules(List<ExtractionRule> rules, List<ManifestDiagnostic> diagnostics) {
        if (rules.isEmpty()) {
            diagnostics.add(diagnostic("MANIFEST_EMPTY_EXTRACTION_RULES", "No active extraction rules", List.of(), "extractionRules"));
        }
        Set<String> messageTypes = new HashSet<>();
        Set<String> routingFieldNames = new HashSet<>();
        for (ExtractionRule rule : rules) {
            if (!messageTypes.add(rule.messageType())) {
                diagnostics.add(diagnostic("MANIFEST_DUPLICATE_MESSAGE_TYPE",
                        "Duplicate message type " + rule.messageType(), List.of(rule.id()), "extractionRules"));
            }
            Set<String> fieldNames = new HashSet<>();
            for (FieldBinding binding : concat(rule.routingFields(), rule.extraFields())) {
                if (!binding.isValid()) {
                    diagnostics.add(diagnostic("MANIFEST_FIELD_BINDING_INVALID",
                            "Field binding must set exactly one of parent+key or path",
                            List.of(rule.id()), "extractionRules." + rule.messageType()));
                }
                if (!fieldNames.add(binding.name())) {
                    diagnostics.add(diagnostic("MANIFEST_DUPLICATE_FIELD_NAME",
                            "Duplicate field name " + binding.name(),
                            List.of(rule.id()), "extractionRules." + rule.messageType()));
                }
            }
            rule.routingFields().forEach(b -> routingFieldNames.add(b.name()));
        }
        return routingFieldNames;
    }

    private void validateTkbPayList(List<TkbPayListEntry> entries, List<ManifestDiagnostic> diagnostics) {
        Set<String> ids = new HashSet<>();
        for (TkbPayListEntry entry : entries) {
            if (!ids.add(entry.rcvTspId())) {
                diagnostics.add(diagnostic("MANIFEST_DUPLICATE_TKB_PAY_ENTRY",
                        "Duplicate TKB-Pay entry " + entry.rcvTspId(), List.of(entry.id()), "terminals.tkbPayList"));
            }
        }
    }

    private void validateTerminal(TerminalRoutingConfig terminal, Set<String> routingFieldNames,
                                  List<ManifestDiagnostic> diagnostics) {
        if (terminal == null) {
            diagnostics.add(diagnostic("MANIFEST_TERMINAL_CONFIG_MISSING",
                    "Exactly one active terminal routing config is required", List.of(), "terminals"));
            return;
        }
        List<UUID> ids = List.of(terminal.id());
        if (terminal.c2bFieldName() == null || !routingFieldNames.contains(terminal.c2bFieldName())) {
            diagnostics.add(diagnostic("MANIFEST_ROUTING_FIELD_UNRESOLVED",
                    "c2bFieldName does not match any active routing field", ids, "terminals.c2bFieldName"));
        }
        if (terminal.b2cFieldName() == null || !routingFieldNames.contains(terminal.b2cFieldName())) {
            diagnostics.add(diagnostic("MANIFEST_ROUTING_FIELD_UNRESOLVED",
                    "b2cFieldName does not match any active routing field", ids, "terminals.b2cFieldName"));
        }
    }

    private RoutingManifestContent buildContent(ProspectiveConfig config) {
        Map<String, ExtractionRulePayload> extractionRules = new TreeMap<>();
        for (ExtractionRule rule : config.extractionRules()) {
            extractionRules.put(rule.messageType(),
                    new ExtractionRulePayload(List.copyOf(rule.routingFields()), List.copyOf(rule.extraFields())));
        }

        Map<String, UpstreamPayload> upstreams = new TreeMap<>();
        for (Upstream u : config.upstreams()) {
            RetryPayload retry = (u.retryMaxAttempts() == null && u.retryBackoffMs() == null)
                    ? null : new RetryPayload(u.retryMaxAttempts(), u.retryBackoffMs());
            upstreams.put(u.name(), new UpstreamPayload(u.url(), u.timeoutMs(), retry));
        }

        Map<String, String> routing = new TreeMap<>();
        for (RoutingFlag flag : config.routingFlags()) {
            routing.put(flag.key(), flag.value());
        }

        List<String> tkbPayList = config.tkbPayList().stream()
                .map(TkbPayListEntry::rcvTspId)
                .sorted()
                .toList();

        TerminalRoutingConfig t = config.terminalConfig();
        TerminalsPayload terminals = new TerminalsPayload(
                t.c2bFieldName(), t.b2cFieldName(), t.tkbPayPrefix(), tkbPayList);

        return new RoutingManifestContent(extractionRules, terminals, routing, upstreams);
    }

    private static List<FieldBinding> concat(List<FieldBinding> a, List<FieldBinding> b) {
        List<FieldBinding> all = new ArrayList<>(a == null ? List.of() : a);
        all.addAll(b == null ? List.of() : b);
        return all;
    }

    private static boolean isAbsoluteUrl(String url) {
        try {
            return URI.create(url).isAbsolute();
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private ManifestDiagnostic diagnostic(String code, String message, List<UUID> entityIds, String path) {
        return new ManifestDiagnostic(code, ManifestDiagnosticSeverity.ERROR, message, entityIds, path);
    }
}
