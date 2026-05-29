package ru.copperside.sbprouter.management.traffic.adapter.in.messaging;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import ru.copperside.sbprouter.management.traffic.application.TrafficIngestService;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "traffic.kafka", name = "enabled", havingValue = "true")
public class TrafficKafkaConsumer {

    private final TrafficIngestService ingestService;
    private final TrafficEventMapper mapper;

    public TrafficKafkaConsumer(TrafficIngestService ingestService, TrafficEventMapper mapper) {
        this.ingestService = ingestService;
        this.mapper = mapper;
    }

    @KafkaListener(
            topics = "${traffic.kafka.topic}",
            groupId = "${traffic.kafka.group-id}",
            containerFactory = "trafficKafkaListenerContainerFactory")
    public void onMessage(ConsumerRecord<String, byte[]> record, Acknowledgment ack) {
        Map<String, String> headers = new HashMap<>();
        for (Header h : record.headers()) {
            headers.put(h.key(), h.value() == null ? null : new String(h.value(), StandardCharsets.UTF_8));
        }
        ingestService.record(mapper.map(record.key(), headers, record.value()));
        ack.acknowledge();
    }
}
