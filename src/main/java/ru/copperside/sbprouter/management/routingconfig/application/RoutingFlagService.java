package ru.copperside.sbprouter.management.routingconfig.application;

import ru.copperside.sbprouter.management.routingconfig.application.port.out.RoutingFlagRepository;
import ru.copperside.sbprouter.management.routingconfig.domain.ConfigStatus;
import ru.copperside.sbprouter.management.routingconfig.domain.RoutingConfigProblemException;
import ru.copperside.sbprouter.management.routingconfig.domain.RoutingFlag;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class RoutingFlagService {

    private final RoutingFlagRepository repository;
    private final Clock clock;

    public RoutingFlagService(RoutingFlagRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public List<RoutingFlag> list() {
        return repository.findAll();
    }

    public RoutingFlag set(String key, String value) {
        if (key == null || key.isBlank()) {
            throw new RoutingConfigProblemException("VALIDATION_ERROR", "flag key is required");
        }
        Instant now = Instant.now(clock);
        return repository.findWorkingByKey(key)
                .filter(existing -> existing.status() == ConfigStatus.DRAFT)
                .map(existing -> repository.save(new RoutingFlag(existing.id(), key, value,
                        ConfigStatus.DRAFT, false, existing.createdAt(), now)))
                .orElseGet(() -> repository.save(new RoutingFlag(UUID.randomUUID(), key, value,
                        ConfigStatus.DRAFT, false, now, now)));
    }
}
