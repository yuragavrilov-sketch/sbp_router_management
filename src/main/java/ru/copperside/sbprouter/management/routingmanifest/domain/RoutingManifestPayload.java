package ru.copperside.sbprouter.management.routingmanifest.domain;

import java.time.Instant;
import java.util.List;

public record RoutingManifestPayload(
        int version,
        RoutingManifestStatus status,
        Instant createdAt,
        RoutingManifestContent content,
        List<ManifestDiagnostic> diagnostics
) {
}
