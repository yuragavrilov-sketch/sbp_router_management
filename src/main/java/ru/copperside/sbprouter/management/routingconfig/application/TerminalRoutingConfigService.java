package ru.copperside.sbprouter.management.routingconfig.application;

import ru.copperside.sbprouter.management.routingconfig.application.port.out.TerminalRoutingConfigRepository;
import ru.copperside.sbprouter.management.routingconfig.domain.ConfigStatus;
import ru.copperside.sbprouter.management.routingconfig.domain.TerminalRoutingConfig;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public class TerminalRoutingConfigService {

    private final TerminalRoutingConfigRepository repository;
    private final Clock clock;

    public TerminalRoutingConfigService(TerminalRoutingConfigRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public Optional<TerminalRoutingConfig> get() {
        return repository.findWorking();
    }

    public TerminalRoutingConfig save(SaveTerminalConfigCommand command) {
        Instant now = Instant.now(clock);
        Optional<TerminalRoutingConfig> working = repository.findWorking();
        if (working.isPresent() && working.get().status() == ConfigStatus.DRAFT) {
            TerminalRoutingConfig existing = working.get();
            return repository.save(new TerminalRoutingConfig(existing.id(),
                    command.c2bFieldName(), command.b2cFieldName(), command.tkbPayPrefix(),
                    ConfigStatus.DRAFT, false, existing.version(), existing.createdAt(), now));
        }
        return repository.save(new TerminalRoutingConfig(UUID.randomUUID(),
                command.c2bFieldName(), command.b2cFieldName(), command.tkbPayPrefix(),
                ConfigStatus.DRAFT, false, 1, now, now));
    }
}
