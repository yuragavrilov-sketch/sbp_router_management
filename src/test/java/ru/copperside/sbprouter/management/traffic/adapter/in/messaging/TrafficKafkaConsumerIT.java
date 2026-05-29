package ru.copperside.sbprouter.management.traffic.adapter.in.messaging;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import ru.copperside.sbprouter.management.support.PostgresTestSupport;
import ru.copperside.sbprouter.management.traffic.application.port.out.TrafficQueryRepository;
import ru.copperside.sbprouter.management.traffic.domain.TrafficStatus;
import ru.copperside.sbprouter.management.traffic.domain.TrafficTransaction;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Regression integration test: verifies that @KafkaListener is actually wired.
 * Fails if @EnableKafka is removed from TrafficKafkaConfig.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@EmbeddedKafka(partitions = 1, topics = "sbp-router-traffic")
class TrafficKafkaConsumerIT extends PostgresTestSupport {

    private static final String TOPIC = "sbp-router-traffic";
    private static final String CORR_ID = "corr-it-1";

    @DynamicPropertySource
    static void kafkaAndFlags(DynamicPropertyRegistry registry) {
        // Enable Kafka consumer with embedded broker
        registry.add("traffic.kafka.enabled", () -> "true");
        registry.add("traffic.kafka.bootstrap-servers",
                () -> System.getProperty("spring.embedded.kafka.brokers"));
        registry.add("traffic.kafka.topic", () -> TOPIC);
        // Disable cloud config and vault (no external infra needed in tests)
        registry.add("spring.cloud.config.enabled", () -> "false");
        registry.add("spring.cloud.vault.enabled", () -> "false");
    }

    @Autowired
    KafkaListenerEndpointRegistry listenerRegistry;

    @Autowired
    TrafficQueryRepository trafficQueryRepository;

    private KafkaProducer<String, byte[]> producer;

    @AfterEach
    void cleanup() {
        if (producer != null) {
            producer.close();
        }
    }

    @Test
    void kafkaListenerIsWiredAndConsumesRequestAndResponse() throws Exception {
        // 1. Wait for partition assignment so we don't miss messages (auto.offset.reset=latest)
        List<MessageListenerContainer> containers = listenerRegistry.getListenerContainers()
                .stream().toList();
        assertThat(containers)
                .as("@KafkaListener containers must be registered — fails if @EnableKafka is missing")
                .isNotEmpty();

        for (MessageListenerContainer container : containers) {
            ContainerTestUtils.waitForAssignment(container, 1);
        }

        // 2. Produce request then response records
        String brokers = System.getProperty("spring.embedded.kafka.brokers");
        producer = new KafkaProducer<>(Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, brokers,
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName(),
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName()
        ));

        ProducerRecord<String, byte[]> requestRecord = new ProducerRecord<>(
                TOPIC, null, CORR_ID, "<req/>".getBytes(StandardCharsets.UTF_8));
        addHeader(requestRecord, "direction", "request");
        addHeader(requestRecord, "txId", "tx-it-1");
        addHeader(requestRecord, "correlationId", CORR_ID);
        addHeader(requestRecord, "requestType", "ReqAuthPay");
        addHeader(requestRecord, "env", "test");
        addHeader(requestRecord, "timestamp", "2026-05-29T10:00:00Z");
        addHeader(requestRecord, "terminalOwner", "owner-IT");
        addHeader(requestRecord, "route", "route-it");
        producer.send(requestRecord).get();

        ProducerRecord<String, byte[]> responseRecord = new ProducerRecord<>(
                TOPIC, null, CORR_ID, "<ans/>".getBytes(StandardCharsets.UTF_8));
        addHeader(responseRecord, "direction", "response");
        addHeader(responseRecord, "txId", "tx-it-1");
        addHeader(responseRecord, "correlationId", CORR_ID);
        addHeader(responseRecord, "requestType", "ReqAuthPay");
        addHeader(responseRecord, "env", "test");
        addHeader(responseRecord, "timestamp", "2026-05-29T10:00:00.040Z");
        addHeader(responseRecord, "upstream", "infosrv");
        addHeader(responseRecord, "outcome", "ok");
        producer.send(responseRecord).get();

        // 3. Poll up to 15 s for the consumer to persist both events
        TrafficTransaction tx = null;
        long deadline = System.currentTimeMillis() + 15_000;
        while (System.currentTimeMillis() < deadline) {
            Optional<TrafficTransaction> found = trafficQueryRepository.find(CORR_ID);
            if (found.isPresent() && found.get().status() == TrafficStatus.RESPONDED) {
                tx = found.get();
                break;
            }
            Thread.sleep(200);
        }

        if (tx == null) {
            fail("TrafficTransaction corr-it-1 not found within 15 s — " +
                    "@KafkaListener may not be wired (check @EnableKafka on TrafficKafkaConfig)");
        }

        // 4. Assert persisted state
        assertThat(tx.status()).isEqualTo(TrafficStatus.RESPONDED);
        assertThat(tx.latencyMs()).isEqualTo(40L);
        assertThat(tx.upstream()).isEqualTo("infosrv");
        assertThat(tx.terminalOwner()).isEqualTo("owner-IT");
        assertThat(tx.requestXml()).isEqualTo("<req/>");
        assertThat(tx.responseXml()).isEqualTo("<ans/>");
    }

    private static void addHeader(ProducerRecord<?, ?> record, String key, String value) {
        record.headers().add(new RecordHeader(key, value.getBytes(StandardCharsets.UTF_8)));
    }
}
