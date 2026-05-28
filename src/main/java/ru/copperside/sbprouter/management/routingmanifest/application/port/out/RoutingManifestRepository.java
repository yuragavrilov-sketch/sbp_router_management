package ru.copperside.sbprouter.management.routingmanifest.application.port.out;

import ru.copperside.sbprouter.management.routingmanifest.domain.ProspectiveConfig;
import ru.copperside.sbprouter.management.routingmanifest.domain.RoutingManifest;

import java.util.Optional;
import java.util.UUID;

public interface RoutingManifestRepository {

    int nextVersion();

    Optional<Integer> latestVersion();

    /**
     * In one transaction: persist the manifest and its entity rows, then promote the
     * prospective config rows to ACTIVE and archive every other previously-active or
     * leftover draft row.
     */
    RoutingManifest publish(RoutingManifest manifest, ProspectiveConfig prospective);

    Optional<RoutingManifest> findLatest();

    Optional<RoutingManifest> find(UUID id);
}
