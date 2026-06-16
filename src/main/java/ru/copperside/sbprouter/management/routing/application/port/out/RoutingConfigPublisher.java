package ru.copperside.sbprouter.management.routing.application.port.out;

import ru.copperside.sbprouter.management.routing.domain.RoutingConfig;

public interface RoutingConfigPublisher {
    void publish(RoutingConfig config);
}
