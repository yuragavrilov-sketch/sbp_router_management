package ru.copperside.sbprouter.management.routingconfig.domain;

import java.time.Instant;
import java.util.UUID;

public record RoutingFlag(
        UUID id,
        String key,
        String value,
        ConfigStatus status,
        boolean removal,
        Instant createdAt,
        Instant updatedAt
) {
}
