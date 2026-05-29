package ru.copperside.sbprouter.management.routingconfig.application;

import ru.copperside.sbprouter.management.routingconfig.application.port.out.UpstreamRepository;
import ru.copperside.sbprouter.management.routingconfig.domain.ConfigStatus;
import ru.copperside.sbprouter.management.routingconfig.domain.RoutingConfigProblemException;
import ru.copperside.sbprouter.management.routingconfig.domain.Upstream;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class UpstreamService {

    private final UpstreamRepository repository;
    private final Clock clock;

    public UpstreamService(UpstreamRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public List<Upstream> list() {
        return repository.findAll();
    }

    public Upstream create(CreateUpstreamCommand command) {
        if (command.name() == null || command.name().isBlank()) {
            throw new RoutingConfigProblemException("VALIDATION_ERROR", "Upstream name is required");
        }
        Instant now = Instant.now(clock);
        Upstream draft = new Upstream(
                UUID.randomUUID(),
                command.name(),
                command.url(),
                command.timeoutMs(),
                command.retryMaxAttempts(),
                command.retryBackoffMs(),
                ConfigStatus.DRAFT,
                false,
                1,
                now,
                now);
        return repository.save(draft);
    }

    public Upstream patch(UUID id, PatchUpstreamCommand command) {
        Upstream existing = repository.findById(id)
                .orElseThrow(() -> new RoutingConfigProblemException("UPSTREAM_NOT_FOUND", "Upstream not found"));
        Upstream patched = existing.withPatch(
                command.url(), command.timeoutMs(), command.retryMaxAttempts(), command.retryBackoffMs(),
                Instant.now(clock));
        return repository.save(patched);
    }

    public Upstream markRemoval(UUID id) {
        Upstream existing = repository.findById(id)
                .orElseThrow(() -> new RoutingConfigProblemException("UPSTREAM_NOT_FOUND", "Upstream not found"));
        return repository.save(new Upstream(
                existing.id(), existing.name(), existing.url(), existing.timeoutMs(),
                existing.retryMaxAttempts(), existing.retryBackoffMs(),
                existing.status(), true, existing.version(), existing.createdAt(), Instant.now(clock)));
    }
}
