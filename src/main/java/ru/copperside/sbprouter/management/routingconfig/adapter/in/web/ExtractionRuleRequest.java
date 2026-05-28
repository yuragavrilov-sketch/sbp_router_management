package ru.copperside.sbprouter.management.routingconfig.adapter.in.web;

import ru.copperside.sbprouter.management.routingconfig.domain.FieldBinding;

import java.util.List;

public record ExtractionRuleRequest(
        String messageType,
        List<FieldBinding> routingFields,
        List<FieldBinding> extraFields
) {
}
