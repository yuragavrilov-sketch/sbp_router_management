package ru.copperside.sbprouter.management.routingconfig.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ExtractionRule(
        UUID id,
        String messageType,
        List<FieldBinding> routingFields,
        List<FieldBinding> extraFields,
        ConfigStatus status,
        boolean removal,
        int version,
        Instant createdAt,
        Instant updatedAt
) {
}
