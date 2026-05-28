package ru.copperside.sbprouter.management.routingconfig.application.port.out;

import ru.copperside.sbprouter.management.routingconfig.domain.ExtractionRule;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExtractionRuleRepository {
    ExtractionRule save(ExtractionRule rule);

    Optional<ExtractionRule> findById(UUID id);

    List<ExtractionRule> findAll();
}
