package ru.copperside.sbprouter.management.fleet.adapter.in.messaging;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import ru.copperside.sbprouter.management.fleet.application.FleetRegistry;
import ru.copperside.sbprouter.management.fleet.domain.RouterInstance;

/**
 * Consumes router heartbeats (gated by the same flag as traffic ingest) into the in-memory
 * {@link FleetRegistry}. Unparseable heartbeats are dropped (acked) — they must never wedge the
 * partition.
 */
@Component
@ConditionalOnProperty(prefix = "traffic.kafka", name = "enabled", havingValue = "true")
public class HeartbeatKafkaConsumer {

    private final FleetRegistry registry;
    private final HeartbeatMapper mapper;

    public HeartbeatKafkaConsumer(FleetRegistry registry, HeartbeatMapper mapper) {
        this.registry = registry;
        this.mapper = mapper;
    }

    @KafkaListener(
            topics = "${fleet.topic:sbp-router-heartbeat}",
            containerFactory = "fleetKafkaListenerContainerFactory")
    public void onMessage(ConsumerRecord<String, byte[]> record, Acknowledgment ack) {
        RouterInstance instance = mapper.map(record.value());
        if (instance != null) {
            registry.record(instance);
        }
        ack.acknowledge();
    }
}
