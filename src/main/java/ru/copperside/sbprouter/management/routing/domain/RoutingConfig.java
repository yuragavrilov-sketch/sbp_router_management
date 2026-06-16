package ru.copperside.sbprouter.management.routing.domain;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RoutingConfig(Long version, String activeGroup, Map<String, Group> groups) {

    public record Group(List<String> backends) {
    }

    public RoutingConfig withVersion(long v) {
        return new RoutingConfig(v, activeGroup, groups);
    }
}
