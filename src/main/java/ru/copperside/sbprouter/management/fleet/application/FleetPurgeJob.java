package ru.copperside.sbprouter.management.fleet.application;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.copperside.sbprouter.management.fleet.config.FleetProperties;

import java.time.Clock;

/**
 * Periodically evicts long-silent router instances so the in-memory registry doesn't grow without
 * bound (e.g. pods that were scaled down). Entries become "stale" in the API after one TTL; this
 * job removes them entirely after a generous multiple of the TTL.
 */
@Component
public class FleetPurgeJob {

    private static final int PURGE_TTL_MULTIPLIER = 10;

    private final FleetRegistry registry;
    private final FleetProperties properties;
    private final Clock clock;

    public FleetPurgeJob(FleetRegistry registry, FleetProperties properties, Clock clock) {
        this.registry = registry;
        this.properties = properties;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${fleet.purge-interval-ms:60000}")
    public void purge() {
        registry.purgeOlderThan(clock.instant().minus(properties.ttl().multipliedBy(PURGE_TTL_MULTIPLIER)));
    }
}
