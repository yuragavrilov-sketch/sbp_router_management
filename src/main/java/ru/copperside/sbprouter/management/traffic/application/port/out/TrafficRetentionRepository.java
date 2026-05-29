package ru.copperside.sbprouter.management.traffic.application.port.out;

import java.time.Instant;

public interface TrafficRetentionRepository {
    /** Delete transactions created strictly before the cutoff; returns rows deleted. */
    int deleteOlderThan(Instant cutoff);
}
