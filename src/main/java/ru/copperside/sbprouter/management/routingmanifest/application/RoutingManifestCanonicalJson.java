package ru.copperside.sbprouter.management.routingmanifest.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import ru.copperside.sbprouter.management.routingmanifest.domain.RoutingManifestContent;
import ru.copperside.sbprouter.management.routingmanifest.domain.RoutingManifestPayload;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public class RoutingManifestCanonicalJson {

    private final ObjectMapper objectMapper;

    public RoutingManifestCanonicalJson() {
        this.objectMapper = JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
                .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
                .build();
    }

    public byte[] payloadBytes(RoutingManifestPayload payload) {
        return bytes(payload);
    }

    /** Checksum is computed over content only, so it is purely content-derived. */
    public String checksum(RoutingManifestContent content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return "sha256:" + HexFormat.of().formatHex(digest.digest(bytes(content)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Cannot calculate routing manifest checksum", e);
        }
    }

    private byte[] bytes(Object value) {
        try {
            return objectMapper.writeValueAsBytes(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize routing manifest value", e);
        }
    }
}
