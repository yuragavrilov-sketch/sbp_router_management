package ru.copperside.sbprouter.management.routing.domain;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RoutingConfig(Long version, String activeGroup, Map<String, Group> groups, AuthPay authPay) {

    public record Group(List<String> backends) {
    }

    /** Optional route: when enabled, ReqAuthPay messages are forwarded to this backend pool.
     *  When {@code sbpOperations} is non-null and non-empty, only ReqAuthPay messages whose
     *  SbpOperation is in the list are routed to this pool. */
    public record AuthPay(boolean enabled, List<String> backends, Integer timeoutMs, List<String> sbpOperations) {
        /** Backward-compatible secondary constructor for call sites that predate sbpOperations. */
        public AuthPay(boolean enabled, List<String> backends, Integer timeoutMs) {
            this(enabled, backends, timeoutMs, null);
        }
    }

    /** Backward-compatible constructor for call sites that predate the authPay route. */
    public RoutingConfig(Long version, String activeGroup, Map<String, Group> groups) {
        this(version, activeGroup, groups, null);
    }

    public RoutingConfig withVersion(long v) {
        return new RoutingConfig(v, activeGroup, groups, authPay);
    }
}
