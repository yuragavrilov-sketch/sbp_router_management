package ru.copperside.sbprouter.management.routingmanifest.domain;

import java.util.List;
import java.util.UUID;

public record ManifestDiagnostic(
        String code,
        ManifestDiagnosticSeverity severity,
        String message,
        List<UUID> entityIds,
        String path
) {
}
