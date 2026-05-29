package ru.copperside.sbprouter.management.traffic.application;

import ru.copperside.sbprouter.management.traffic.application.port.out.TrafficRetentionRepository;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

public class TrafficRetentionService {

    private final TrafficRetentionRepository repository;
    private final Clock clock;
    private final int retentionDays;

    public TrafficRetentionService(TrafficRetentionRepository repository, Clock clock, int retentionDays) {
        this.repository = repository;
        this.clock = clock;
        this.retentionDays = retentionDays;
    }

    public int purge() {
        Instant cutoff = Instant.now(clock).minus(Duration.ofDays(retentionDays));
        return repository.deleteOlderThan(cutoff);
    }
}
