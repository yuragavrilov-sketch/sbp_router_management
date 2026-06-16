package ru.copperside.sbprouter.management.routing.adapter.out.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import ru.copperside.sbprouter.management.routing.config.RoutingConfigProperties;
import ru.copperside.sbprouter.management.routing.domain.RoutingConfig;
import ru.copperside.sbprouter.management.traffic.config.TrafficProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@EmbeddedKafka(partitions = 1, topics = "sbp-router-routing-config")
class KafkaRoutingConfigPublisherTest {

    private static final String TOPIC = "sbp-router-routing-config";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    EmbeddedKafkaBroker broker;

    private KafkaRoutingConfigPublisher publisher;
    private KafkaConsumer<String, byte[]> consumer;

    @AfterEach
    void cleanup() {
        if (publisher != null) publisher.close();
        if (consumer != null) consumer.close();
    }

    private KafkaRoutingConfigPublisher buildPublisher() {
        TrafficProperties traffic = new TrafficProperties(
                new TrafficProperties.Kafka(true, broker.getBrokersAsString(), "sbp-router-traffic", "test-group"),
                30);
        RoutingConfigProperties props = new RoutingConfigProperties(TOPIC);
        return new KafkaRoutingConfigPublisher(traffic, props);
    }

    private KafkaConsumer<String, byte[]> buildConsumer() {
        Map<String, Object> cfg = new HashMap<>();
        cfg.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, broker.getBrokersAsString());
        cfg.put(ConsumerConfig.GROUP_ID_CONFIG, "test-routing-config-consumer-" + System.nanoTime());
        cfg.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        cfg.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
        cfg.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        KafkaConsumer<String, byte[]> c = new KafkaConsumer<>(cfg);
        c.subscribe(List.of(TOPIC));
        return c;
    }

    @Test
    void publishWritesRoutingConfigToTopic() throws Exception {
        publisher = buildPublisher();
        consumer = buildConsumer();

        RoutingConfig config = new RoutingConfig(3L, "default",
                Map.of("default", new RoutingConfig.Group(List.of("http://infosrv/api"))));

        publisher.publish(config);

        List<ConsumerRecord<String, byte[]>> received = new ArrayList<>();
        long deadline = System.currentTimeMillis() + 10_000;
        while (System.currentTimeMillis() < deadline && received.isEmpty()) {
            ConsumerRecords<String, byte[]> records = consumer.poll(Duration.ofMillis(300));
            records.forEach(received::add);
        }

        assertThat(received).hasSize(1);
        ConsumerRecord<String, byte[]> record = received.get(0);
        assertThat(record.key()).isEqualTo("routing-config");

        JsonNode node = MAPPER.readTree(record.value());
        assertThat(node.path("version").asLong()).isEqualTo(3L);
        assertThat(node.path("activeGroup").asText()).isEqualTo("default");
        assertThat(node.path("groups").has("default")).isTrue();
    }
}
