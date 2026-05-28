package ru.copperside.sbprouter.management.routingconfig.application;

import ru.copperside.sbprouter.management.routingconfig.application.port.out.TkbPayListRepository;
import ru.copperside.sbprouter.management.routingconfig.domain.ConfigStatus;
import ru.copperside.sbprouter.management.routingconfig.domain.RoutingConfigProblemException;
import ru.copperside.sbprouter.management.routingconfig.domain.TkbPayListEntry;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class TkbPayListService {

    private final TkbPayListRepository repository;
    private final Clock clock;

    public TkbPayListService(TkbPayListRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public List<TkbPayListEntry> list() {
        return repository.findAll();
    }

    public TkbPayListEntry add(String rcvTspId) {
        if (rcvTspId == null || rcvTspId.isBlank()) {
            throw new RoutingConfigProblemException("VALIDATION_ERROR", "rcvTspId is required");
        }
        Instant now = Instant.now(clock);
        return repository.save(new TkbPayListEntry(UUID.randomUUID(), rcvTspId, ConfigStatus.DRAFT, false, now, now));
    }

    public TkbPayListEntry markRemoval(UUID id) {
        TkbPayListEntry existing = repository.findById(id)
                .orElseThrow(() -> new RoutingConfigProblemException("TKB_PAY_ENTRY_NOT_FOUND", "TKB-Pay entry not found"));
        return repository.save(new TkbPayListEntry(existing.id(), existing.rcvTspId(),
                existing.status(), true, existing.createdAt(), Instant.now(clock)));
    }
}
