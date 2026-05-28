package ru.copperside.sbprouter.management.routingmanifest.domain;

import java.time.Instant;
import java.util.UUID;

public record RoutingManifest(
        UUID id,
        int version,
        RoutingManifestStatus status,
        String checksum,
        Instant createdAt,
        RoutingManifestContent content,
        RoutingManifestPayload payload
) {
}
