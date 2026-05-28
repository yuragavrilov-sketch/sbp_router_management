package ru.copperside.sbprouter.management.routingconfig.domain;

import java.time.Instant;
import java.util.UUID;

public record TkbPayListEntry(
        UUID id,
        String rcvTspId,
        ConfigStatus status,
        boolean removal,
        Instant createdAt,
        Instant updatedAt
) {
}
