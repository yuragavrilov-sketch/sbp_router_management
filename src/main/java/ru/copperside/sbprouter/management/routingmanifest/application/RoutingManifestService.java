package ru.copperside.sbprouter.management.routingmanifest.application;

import ru.copperside.sbprouter.management.routingconfig.application.port.out.ExtractionRuleRepository;
import ru.copperside.sbprouter.management.routingconfig.application.port.out.RoutingFlagRepository;
import ru.copperside.sbprouter.management.routingconfig.application.port.out.TerminalRoutingConfigRepository;
import ru.copperside.sbprouter.management.routingconfig.application.port.out.TkbPayListRepository;
import ru.copperside.sbprouter.management.routingconfig.application.port.out.UpstreamRepository;
import ru.copperside.sbprouter.management.routingmanifest.application.port.out.RoutingManifestRepository;
import ru.copperside.sbprouter.management.routingmanifest.application.port.out.ManifestPublishedNotifier;
import ru.copperside.sbprouter.management.routingmanifest.domain.PendingChanges;
import ru.copperside.sbprouter.management.routingmanifest.domain.ProspectiveConfig;
import ru.copperside.sbprouter.management.routingmanifest.domain.RoutingManifest;
import ru.copperside.sbprouter.management.routingmanifest.domain.RoutingManifestProblemException;

import java.util.Optional;
import java.util.UUID;

public class RoutingManifestService {

    private final UpstreamRepository upstreams;
    private final ExtractionRuleRepository extractionRules;
    private final TerminalRoutingConfigRepository terminalConfig;
    private final TkbPayListRepository tkbPayList;
    private final RoutingFlagRepository routingFlags;
    private final RoutingManifestRepository manifests;
    private final ProspectiveConfigAssembler assembler;
    private final RoutingManifestCompiler compiler;
    private final ManifestPublishedNotifier notifier;

    public RoutingManifestService(
            UpstreamRepository upstreams,
            ExtractionRuleRepository extractionRules,
            TerminalRoutingConfigRepository terminalConfig,
            TkbPayListRepository tkbPayList,
            RoutingFlagRepository routingFlags,
            RoutingManifestRepository manifests,
            ProspectiveConfigAssembler assembler,
            RoutingManifestCompiler compiler,
            ManifestPublishedNotifier notifier) {
        this.upstreams = upstreams;
        this.extractionRules = extractionRules;
        this.terminalConfig = terminalConfig;
        this.tkbPayList = tkbPayList;
        this.routingFlags = routingFlags;
        this.manifests = manifests;
        this.assembler = assembler;
        this.compiler = compiler;
        this.notifier = notifier;
    }

    public RoutingManifest publish() {
        ProspectiveConfig prospective = assembler.assemble(
                upstreams.findAll(), extractionRules.findAll(), terminalConfig.findWorking().stream().toList(),
                tkbPayList.findAll(), routingFlags.findAll());
        int version = manifests.nextVersion();
        RoutingManifest manifest = compiler.compile(version, prospective);
        Optional<RoutingManifest> latest = manifests.findLatest();
        if (latest.filter(l -> l.checksum().equals(manifest.checksum())).isPresent()) {
            return latest.get();
        }
        RoutingManifest published = manifests.publish(manifest, prospective);
        notifier.published(published);
        return published;
    }

    public PendingChanges pendingChanges() {
        int next = manifests.nextVersion();
        Integer current = manifests.latestVersion().orElse(null);
        return assembler.pendingChanges(
                upstreams.findAll(), extractionRules.findAll(), terminalConfig.findWorking().stream().toList(),
                tkbPayList.findAll(), routingFlags.findAll(), next, current);
    }

    public RoutingManifest latest() {
        return manifests.findLatest()
                .orElseThrow(() -> new RoutingManifestProblemException("ROUTING_MANIFEST_NOT_FOUND",
                        "Routing manifest not found"));
    }

    public RoutingManifest get(UUID id) {
        if (id == null) {
            throw new RoutingManifestProblemException("ROUTING_MANIFEST_NOT_FOUND",
                    "Routing manifest not found");
        }
        return manifests.find(id)
                .orElseThrow(() -> new RoutingManifestProblemException("ROUTING_MANIFEST_NOT_FOUND",
                        "Routing manifest not found"));
    }
}
