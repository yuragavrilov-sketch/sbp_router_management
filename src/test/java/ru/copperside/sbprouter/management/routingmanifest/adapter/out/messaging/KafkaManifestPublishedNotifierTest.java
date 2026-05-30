package ru.copperside.sbprouter.management.routingmanifest.adapter.out.messaging;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;
import ru.copperside.sbprouter.management.routingmanifest.domain.RoutingManifest;
import ru.copperside.sbprouter.management.routingmanifest.domain.RoutingManifestStatus;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@SuppressWarnings("unchecked")
class KafkaManifestPublishedNotifierTest {

    @Test
    void sendsVersionAndChecksumToManifestTopic() {
        KafkaTemplate<String, byte[]> template = mock(KafkaTemplate.class);
        KafkaManifestPublishedNotifier notifier = new KafkaManifestPublishedNotifier(template, "sbp-router-manifest");

        RoutingManifest manifest = new RoutingManifest(UUID.randomUUID(), 7, RoutingManifestStatus.VALID,
                "sha256:abc123", Instant.parse("2026-05-30T10:00:00Z"), null, null);

        notifier.published(manifest);

        ArgumentCaptor<byte[]> payload = ArgumentCaptor.forClass(byte[].class);
        verify(template).send(eq("sbp-router-manifest"), eq("7"), payload.capture());
        String json = new String(payload.getValue(), StandardCharsets.UTF_8);
        assertThat(json).contains("\"version\":7").contains("\"checksum\":\"sha256:abc123\"");
    }
}
