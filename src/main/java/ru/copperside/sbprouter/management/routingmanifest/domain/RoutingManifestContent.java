package ru.copperside.sbprouter.management.routingmanifest.domain;

import ru.copperside.sbprouter.management.routingconfig.domain.FieldBinding;

import java.util.List;
import java.util.Map;

public record RoutingManifestContent(
        Map<String, ExtractionRulePayload> extractionRules,
        TerminalsPayload terminals,
        Map<String, String> routing,
        Map<String, UpstreamPayload> upstreams
) {
    public record ExtractionRulePayload(List<FieldBinding> routingFields, List<FieldBinding> extraFields) {
    }

    public record TerminalsPayload(String c2bFieldName, String b2cFieldName, String tkbPayPrefix, List<String> tkbPayList) {
    }

    public record UpstreamPayload(String url, Integer timeoutMs, RetryPayload retry) {
    }

    public record RetryPayload(Integer maxAttempts, Integer backoffMs) {
    }
}
