package ru.copperside.sbprouter.management.routingconfig.application.port.out;

import ru.copperside.sbprouter.management.routingconfig.domain.TkbPayListEntry;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TkbPayListRepository {
    TkbPayListEntry save(TkbPayListEntry entry);

    Optional<TkbPayListEntry> findById(UUID id);

    List<TkbPayListEntry> findAll();
}
