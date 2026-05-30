package ru.copperside.sbprouter.management.routingmanifest.application.port.out;

import ru.copperside.sbprouter.management.routingmanifest.domain.RoutingManifest;

/** Notifies interested parties (e.g. sbp-router via Kafka) that a NEW manifest version was published. */
public interface ManifestPublishedNotifier {

    void published(RoutingManifest manifest);

    /** No-op notifier used when event publishing is disabled or in tests. */
    ManifestPublishedNotifier NOOP = manifest -> { };
}
