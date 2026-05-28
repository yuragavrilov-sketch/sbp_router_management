package ru.copperside.sbprouter.management.routingconfig.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FieldBindingJsonTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void parentKeyBindingSerializesWithExactlyNameParentKey() throws Exception {
        FieldBinding binding = new FieldBinding("terminalName", "PayProfile", "Tran.TermName", null);

        String json = mapper.writeValueAsString(binding);

        @SuppressWarnings("unchecked")
        Map<String, Object> keys = mapper.readValue(json, Map.class);
        assertThat(keys.keySet()).containsExactlyInAnyOrder("name", "parent", "key");
        assertThat(json).doesNotContain("\"valid\"");
        assertThat(json).doesNotContain("\"path\"");
    }

    @Test
    void pathBindingSerializesWithExactlyNamePath() throws Exception {
        FieldBinding binding = new FieldBinding("amount", null, null, "/Document/GCSvc/Amount");

        String json = mapper.writeValueAsString(binding);

        @SuppressWarnings("unchecked")
        Map<String, Object> keys = mapper.readValue(json, Map.class);
        assertThat(keys.keySet()).containsExactlyInAnyOrder("name", "path");
        assertThat(json).doesNotContain("\"valid\"");
        assertThat(json).doesNotContain("\"parent\"");
        assertThat(json).doesNotContain("\"key\"");
    }
}
