package ru.copperside.sbprouter.management.traffic.application.port.out;

import ru.copperside.sbprouter.management.traffic.application.TrafficListResult;
import ru.copperside.sbprouter.management.traffic.application.TrafficQuery;
import ru.copperside.sbprouter.management.traffic.domain.TrafficStats;
import ru.copperside.sbprouter.management.traffic.domain.TrafficTransaction;

import java.time.Instant;
import java.util.Optional;

public interface TrafficQueryRepository {
    TrafficListResult list(TrafficQuery query);

    Optional<TrafficTransaction> find(String correlationId);

    TrafficStats stats(Instant from, Instant to);
}
