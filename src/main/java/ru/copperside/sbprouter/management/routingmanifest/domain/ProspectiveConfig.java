package ru.copperside.sbprouter.management.routingmanifest.domain;

import ru.copperside.sbprouter.management.routingconfig.domain.ExtractionRule;
import ru.copperside.sbprouter.management.routingconfig.domain.RoutingFlag;
import ru.copperside.sbprouter.management.routingconfig.domain.TerminalRoutingConfig;
import ru.copperside.sbprouter.management.routingconfig.domain.TkbPayListEntry;
import ru.copperside.sbprouter.management.routingconfig.domain.Upstream;

import java.util.List;

public record ProspectiveConfig(
        List<Upstream> upstreams,
        List<ExtractionRule> extractionRules,
        TerminalRoutingConfig terminalConfig,
        List<TkbPayListEntry> tkbPayList,
        List<RoutingFlag> routingFlags
) {
}
