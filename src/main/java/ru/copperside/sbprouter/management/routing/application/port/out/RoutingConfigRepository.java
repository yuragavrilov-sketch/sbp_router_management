package ru.copperside.sbprouter.management.routing.application.port.out;

import ru.copperside.sbprouter.management.routing.domain.RoutingConfig;
import java.util.Optional;

public interface RoutingConfigRepository {
    Optional<RoutingConfig> latest();
    long nextVersion();
    void save(RoutingConfig config);
}
