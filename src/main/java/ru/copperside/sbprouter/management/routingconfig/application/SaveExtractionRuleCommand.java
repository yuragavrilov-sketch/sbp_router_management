package ru.copperside.sbprouter.management.routingconfig.application;

import ru.copperside.sbprouter.management.routingconfig.domain.FieldBinding;

import java.util.List;

public record SaveExtractionRuleCommand(
        String messageType,
        List<FieldBinding> routingFields,
        List<FieldBinding> extraFields
) {
}
