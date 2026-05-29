package ru.copperside.sbprouter.management.routingconfig.application;

import ru.copperside.sbprouter.management.routingconfig.application.port.out.ExtractionRuleRepository;
import ru.copperside.sbprouter.management.routingconfig.domain.ConfigStatus;
import ru.copperside.sbprouter.management.routingconfig.domain.ExtractionRule;
import ru.copperside.sbprouter.management.routingconfig.domain.RoutingConfigProblemException;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class ExtractionRuleService {

    private final ExtractionRuleRepository repository;
    private final Clock clock;

    public ExtractionRuleService(ExtractionRuleRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public List<ExtractionRule> list() {
        return repository.findAll();
    }

    public ExtractionRule create(SaveExtractionRuleCommand command) {
        if (command.messageType() == null || command.messageType().isBlank()) {
            throw new RoutingConfigProblemException("VALIDATION_ERROR", "messageType is required");
        }
        Instant now = Instant.now(clock);
        return repository.save(new ExtractionRule(
                UUID.randomUUID(), command.messageType(),
                command.routingFields() == null ? List.of() : command.routingFields(),
                command.extraFields() == null ? List.of() : command.extraFields(),
                ConfigStatus.DRAFT, false, 1, now, now));
    }

    public ExtractionRule patch(UUID id, SaveExtractionRuleCommand command) {
        ExtractionRule existing = repository.findById(id)
                .orElseThrow(() -> new RoutingConfigProblemException("EXTRACTION_RULE_NOT_FOUND", "Extraction rule not found"));
        return repository.save(new ExtractionRule(
                existing.id(),
                command.messageType() != null ? command.messageType() : existing.messageType(),
                command.routingFields() != null ? command.routingFields() : existing.routingFields(),
                command.extraFields() != null ? command.extraFields() : existing.extraFields(),
                existing.status(), existing.removal(), existing.version(), existing.createdAt(), Instant.now(clock)));
    }

    public ExtractionRule markRemoval(UUID id) {
        ExtractionRule existing = repository.findById(id)
                .orElseThrow(() -> new RoutingConfigProblemException("EXTRACTION_RULE_NOT_FOUND", "Extraction rule not found"));
        return repository.save(new ExtractionRule(
                existing.id(), existing.messageType(), existing.routingFields(), existing.extraFields(),
                existing.status(), true, existing.version(), existing.createdAt(), Instant.now(clock)));
    }
}
