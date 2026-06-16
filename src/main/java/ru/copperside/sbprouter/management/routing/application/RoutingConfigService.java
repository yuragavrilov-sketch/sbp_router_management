package ru.copperside.sbprouter.management.routing.application;

import ru.copperside.sbprouter.management.routing.application.port.out.RoutingConfigPublisher;
import ru.copperside.sbprouter.management.routing.application.port.out.RoutingConfigRepository;
import ru.copperside.sbprouter.management.routing.domain.RoutingConfig;

import java.util.Optional;

public class RoutingConfigService {

    private final RoutingConfigRepository repository;
    private final RoutingConfigPublisher publisher;
    private final RoutingConfigValidator validator;

    public RoutingConfigService(RoutingConfigRepository repository, RoutingConfigPublisher publisher,
                                RoutingConfigValidator validator) {
        this.repository = repository;
        this.publisher = publisher;
        this.validator = validator;
    }

    public Optional<RoutingConfig> latest() {
        return repository.latest();
    }

    public RoutingConfig replace(RoutingConfig inbound) {
        validator.validate(inbound);
        RoutingConfig persisted = inbound.withVersion(repository.nextVersion());
        repository.save(persisted);
        publisher.publish(persisted);
        return persisted;
    }
}
