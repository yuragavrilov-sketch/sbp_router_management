package ru.copperside.sbprouter.management.routingconfig.domain;

import java.time.Instant;
import java.util.UUID;

public record TerminalRoutingConfig(
        UUID id,
        String c2bFieldName,
        String b2cFieldName,
        String tkbPayPrefix,
        ConfigStatus status,
        boolean removal,
        int version,
        Instant createdAt,
        Instant updatedAt
) {
}
