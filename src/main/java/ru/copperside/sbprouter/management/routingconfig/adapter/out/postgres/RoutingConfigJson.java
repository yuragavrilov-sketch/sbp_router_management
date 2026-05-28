package ru.copperside.sbprouter.management.routingconfig.adapter.out.postgres;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import ru.copperside.sbprouter.management.routingconfig.domain.FieldBinding;

import java.util.List;

final class RoutingConfigJson {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<List<FieldBinding>> LIST_TYPE = new TypeReference<>() {
    };

    private RoutingConfigJson() {
    }

    static String write(List<FieldBinding> bindings) {
        try {
            return MAPPER.writeValueAsString(bindings == null ? List.of() : bindings);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot serialize field bindings", e);
        }
    }

    static List<FieldBinding> read(String json) {
        try {
            return json == null ? List.of() : MAPPER.readValue(json, LIST_TYPE);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot deserialize field bindings", e);
        }
    }
}
