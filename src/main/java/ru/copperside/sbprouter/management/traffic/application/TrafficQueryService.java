package ru.copperside.sbprouter.management.traffic.application;

import ru.copperside.sbprouter.management.traffic.application.port.out.TrafficQueryRepository;
import ru.copperside.sbprouter.management.traffic.domain.TrafficStats;
import ru.copperside.sbprouter.management.traffic.domain.TrafficTransaction;
import ru.copperside.sbprouter.management.traffic.domain.TrafficTransactionProblemException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

public class TrafficQueryService {

    private final TrafficQueryRepository repository;
    private final Clock clock;

    public TrafficQueryService(TrafficQueryRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public TrafficListResult list(TrafficQuery query) {
        return repository.list(query);
    }

    public TrafficTransaction get(String correlationId) {
        return repository.find(correlationId)
                .orElseThrow(() -> new TrafficTransactionProblemException(
                        "TRAFFIC_TRANSACTION_NOT_FOUND", "Traffic transaction not found"));
    }

    public TrafficStats stats(Instant from, Instant to) {
        Instant now = Instant.now(clock);
        Instant effectiveTo = to != null ? to : now;
        Instant effectiveFrom = from != null ? from : effectiveTo.minus(Duration.ofHours(1));
        return repository.stats(effectiveFrom, effectiveTo);
    }
}
