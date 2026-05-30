package ru.copperside.sbprouter.management.routingmanifest.adapter.out.messaging;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import ru.copperside.sbprouter.management.routingmanifest.application.port.out.ManifestPublishedNotifier;
import ru.copperside.sbprouter.management.routingmanifest.domain.RoutingManifest;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Publishes a trigger-only "manifest published" event ({version, checksum}) to Kafka so
 * sbp-router can fetch-and-apply the latest manifest immediately. Active only when
 * traffic.kafka.enabled=true.
 *
 * Fire-and-forget on a dedicated daemon thread: the send (which can block up to the
 * producer's max.block.ms fetching metadata when the broker is down) must NEVER add latency
 * to or fail the synchronous publish HTTP request. Send failures are logged and swallowed;
 * sbp-router's scheduled poll is the backstop for any lost event.
 */
@Component
@ConditionalOnProperty(prefix = "traffic.kafka", name = "enabled", havingValue = "true")
public class KafkaManifestPublishedNotifier implements ManifestPublishedNotifier {

    private static final Logger log = LoggerFactory.getLogger(KafkaManifestPublishedNotifier.class);

    private final KafkaTemplate<String, byte[]> kafkaTemplate;
    private final String topic;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "manifest-event-publisher");
        t.setDaemon(true);
        return t;
    });

    public KafkaManifestPublishedNotifier(KafkaTemplate<String, byte[]> manifestEventKafkaTemplate,
                                          @Value("${traffic.kafka.manifest-topic:sbp-router-manifest}") String topic) {
        this.kafkaTemplate = manifestEventKafkaTemplate;
        this.topic = topic;
    }

    @Override
    public void published(RoutingManifest manifest) {
        int version = manifest.version();
        String key = String.valueOf(version);
        byte[] payload = ("{\"version\":" + version
                + ",\"checksum\":\"" + manifest.checksum() + "\"}").getBytes(StandardCharsets.UTF_8);
        executor.execute(() -> {
            try {
                kafkaTemplate.send(topic, key, payload);
                log.info("published manifest event v{} to topic {}", version, topic);
            } catch (Exception e) {
                log.warn("failed to publish manifest event v{}: {}", version, e.toString());
            }
        });
    }

    @PreDestroy
    void shutdown() {
        executor.shutdown();
    }
}
