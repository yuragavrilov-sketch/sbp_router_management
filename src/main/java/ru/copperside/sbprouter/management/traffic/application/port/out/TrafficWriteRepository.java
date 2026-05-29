package ru.copperside.sbprouter.management.traffic.application.port.out;

import ru.copperside.sbprouter.management.traffic.domain.TrafficTransaction;

public interface TrafficWriteRepository {
    /** Idempotent upsert keyed by correlationId; null fields must not overwrite existing values. */
    void upsert(TrafficTransaction partial);
}
