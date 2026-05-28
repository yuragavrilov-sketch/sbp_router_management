package ru.copperside.sbprouter.management.routingconfig.application.port.out;

import ru.copperside.sbprouter.management.routingconfig.domain.RoutingFlag;

import java.util.List;
import java.util.Optional;

public interface RoutingFlagRepository {
    RoutingFlag save(RoutingFlag flag);

    Optional<RoutingFlag> findWorkingByKey(String key);

    List<RoutingFlag> findAll();
}
