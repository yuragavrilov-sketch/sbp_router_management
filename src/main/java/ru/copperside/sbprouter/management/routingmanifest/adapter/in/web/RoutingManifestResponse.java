package ru.copperside.sbprouter.management.routingmanifest.adapter.in.web;

import ru.copperside.sbprouter.management.routingmanifest.domain.ManifestDiagnostic;
import ru.copperside.sbprouter.management.routingmanifest.domain.RoutingManifest;
import ru.copperside.sbprouter.management.routingmanifest.domain.RoutingManifestContent;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RoutingManifestResponse(
        UUID id,
        int version,
        String status,
        String checksum,
        Instant createdAt,
        RoutingManifestContent payload,
        List<ManifestDiagnostic> diagnostics
) {
    public static RoutingManifestResponse from(RoutingManifest m) {
        return new RoutingManifestResponse(
                m.id(), m.version(), m.status().name(), m.checksum(), m.createdAt(),
                m.content(), m.payload().diagnostics());
    }
}
