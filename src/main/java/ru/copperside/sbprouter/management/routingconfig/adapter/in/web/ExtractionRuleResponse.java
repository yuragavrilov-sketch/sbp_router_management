package ru.copperside.sbprouter.management.routingconfig.adapter.in.web;

import ru.copperside.sbprouter.management.routingconfig.domain.ExtractionRule;
import ru.copperside.sbprouter.management.routingconfig.domain.FieldBinding;

import java.util.List;
import java.util.UUID;

public record ExtractionRuleResponse(
        UUID id,
        String messageType,
        List<FieldBinding> routingFields,
        List<FieldBinding> extraFields,
        String status,
        boolean removal,
        int version
) {
    public static ExtractionRuleResponse from(ExtractionRule r) {
        return new ExtractionRuleResponse(r.id(), r.messageType(), r.routingFields(), r.extraFields(),
                r.status().name(), r.removal(), r.version());
    }
}
