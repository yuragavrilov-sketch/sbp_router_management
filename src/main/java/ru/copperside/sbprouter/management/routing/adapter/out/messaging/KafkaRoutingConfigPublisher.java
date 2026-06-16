package ru.copperside.sbprouter.management.routing.adapter.out.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import ru.copperside.sbprouter.management.routing.application.port.out.RoutingConfigPublisher;
import ru.copperside.sbprouter.management.routing.config.RoutingConfigProperties;
import ru.copperside.sbprouter.management.routing.domain.RoutingConfig;
import ru.copperside.sbprouter.management.traffic.config.TrafficProperties;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.apache.kafka.clients.producer.ProducerConfig.ACKS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.MAX_BLOCK_MS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG;

@Component
@ConditionalOnProperty(prefix = "traffic.kafka", name = "enabled", havingValue = "true")
public class KafkaRoutingConfigPublisher implements RoutingConfigPublisher, AutoCloseable {

    private static final String KEY = "routing-config";
    private final KafkaProducer<String, byte[]> producer;
    private final String topic;
    private final ObjectMapper mapper = new ObjectMapper();

    public KafkaRoutingConfigPublisher(TrafficProperties traffic, RoutingConfigProperties props) {
        this.topic = props.topic();
        Map<String, Object> cfg = new HashMap<>();
        cfg.put(BOOTSTRAP_SERVERS_CONFIG, traffic.kafka().bootstrapServers());
        cfg.put(KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        cfg.put(VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);
        cfg.put(ACKS_CONFIG, "1");
        cfg.put(MAX_BLOCK_MS_CONFIG, "5000");
        this.producer = new KafkaProducer<>(cfg);
    }

    @Override
    public void publish(RoutingConfig config) {
        try {
            byte[] value = mapper.writeValueAsBytes(config);
            producer.send(new ProducerRecord<>(topic, KEY, value)).get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new IllegalStateException("routing-config publish failed", e);
        }
    }

    @Override
    public void close() {
        producer.close();
    }
}
