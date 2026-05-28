package ru.copperside.sbprouter.management.routingconfig.application.port.out;

import ru.copperside.sbprouter.management.routingconfig.domain.Upstream;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UpstreamRepository {
    Upstream save(Upstream upstream);

    Optional<Upstream> findById(UUID id);

    List<Upstream> findAll();
}
