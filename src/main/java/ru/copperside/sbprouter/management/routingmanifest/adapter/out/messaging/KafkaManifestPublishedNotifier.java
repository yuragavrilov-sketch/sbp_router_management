package ru.copperside.sbprouter.management.routingmanifest.adapter.out.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import ru.copperside.sbprouter.management.routingmanifest.application.port.out.ManifestPublishedNotifier;
import ru.copperside.sbprouter.management.routingmanifest.domain.RoutingManifest;

import java.nio.charset.StandardCharsets;

/**
 * Publishes a trigger-only "manifest published" event ({version, checksum}) to Kafka so
 * sbp-router can fetch-and-apply the latest manifest immediately. Active only when
 * traffic.kafka.enabled=true; never fails the publish (logs and swallows send errors).
 */
@Component
@ConditionalOnProperty(prefix = "traffic.kafka", name = "enabled", havingValue = "true")
public class KafkaManifestPublishedNotifier implements ManifestPublishedNotifier {

    private static final Logger log = LoggerFactory.getLogger(KafkaManifestPublishedNotifier.class);

    private final KafkaTemplate<String, byte[]> kafkaTemplate;
    private final String topic;

    public KafkaManifestPublishedNotifier(KafkaTemplate<String, byte[]> manifestEventKafkaTemplate,
                                          @Value("${traffic.kafka.manifest-topic:sbp-router-manifest}") String topic) {
        this.kafkaTemplate = manifestEventKafkaTemplate;
        this.topic = topic;
    }

    @Override
    public void published(RoutingManifest manifest) {
        String key = String.valueOf(manifest.version());
        byte[] payload = ("{\"version\":" + manifest.version()
                + ",\"checksum\":\"" + manifest.checksum() + "\"}").getBytes(StandardCharsets.UTF_8);
        try {
            kafkaTemplate.send(topic, key, payload);
            log.info("published manifest event v{} to topic {}", manifest.version(), topic);
        } catch (Exception e) {
            log.warn("failed to publish manifest event v{}: {}", manifest.version(), e.toString());
        }
    }
}
