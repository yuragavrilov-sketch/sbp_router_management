package ru.copperside.sbprouter.management.routing.domain;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RoutingConfig(Long version, String activeGroup, Map<String, Group> groups, AuthPay authPay) {

    public record Group(List<String> backends) {
    }

    /** Optional route: when enabled, ReqAuthPay messages are forwarded to this backend pool. */
    public record AuthPay(boolean enabled, List<String> backends, Integer timeoutMs) {
    }

    /** Backward-compatible constructor for call sites that predate the authPay route. */
    public RoutingConfig(Long version, String activeGroup, Map<String, Group> groups) {
        this(version, activeGroup, groups, null);
    }

    public RoutingConfig withVersion(long v) {
        return new RoutingConfig(v, activeGroup, groups, authPay);
    }
}
