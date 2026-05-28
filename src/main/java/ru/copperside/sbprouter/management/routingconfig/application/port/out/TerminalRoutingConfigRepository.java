package ru.copperside.sbprouter.management.routingconfig.application.port.out;

import ru.copperside.sbprouter.management.routingconfig.domain.TerminalRoutingConfig;

import java.util.Optional;

public interface TerminalRoutingConfigRepository {
    TerminalRoutingConfig save(TerminalRoutingConfig config);

    /** The single working row: a DRAFT if one exists, otherwise the ACTIVE row. */
    Optional<TerminalRoutingConfig> findWorking();
}
