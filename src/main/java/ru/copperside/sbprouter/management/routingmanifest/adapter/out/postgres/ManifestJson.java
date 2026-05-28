package ru.copperside.sbprouter.management.routingmanifest.adapter.out.postgres;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.postgresql.util.PGobject;
import ru.copperside.sbprouter.management.routingmanifest.domain.RoutingManifestPayload;

final class ManifestJson {

    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    private ManifestJson() {
    }

    static PGobject jsonb(Object value) {
        PGobject obj = new PGobject();
        obj.setType("jsonb");
        try {
            obj.setValue(MAPPER.writeValueAsString(value));
        } catch (Exception e) {
            throw new IllegalStateException("Cannot serialize manifest json", e);
        }
        return obj;
    }

    static RoutingManifestPayload readPayload(String json) {
        try {
            return MAPPER.readValue(json, RoutingManifestPayload.class);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot deserialize manifest payload", e);
        }
    }
}
