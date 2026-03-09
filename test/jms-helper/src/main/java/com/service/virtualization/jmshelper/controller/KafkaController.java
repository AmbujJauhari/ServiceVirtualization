package com.service.virtualization.jmshelper.controller;

import com.service.virtualization.jmshelper.model.ConsumeResponse;
import com.service.virtualization.jmshelper.model.KafkaConsumeRequest;
import com.service.virtualization.jmshelper.model.KafkaPublishRequest;
import com.service.virtualization.jmshelper.service.KafkaHelperService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * HTTP endpoints for Kafka message publish / consume (E2E test helper only).
 *
 * POST /kafka/publish  — publish a message to a Kafka topic
 * POST /kafka/consume  — poll a Kafka topic for a message (blocks up to timeoutMs)
 */
@RestController
@RequestMapping("/kafka")
public class KafkaController {

    private static final Logger log = LoggerFactory.getLogger(KafkaController.class);

    private final KafkaHelperService kafkaHelperService;

    public KafkaController(KafkaHelperService kafkaHelperService) {
        this.kafkaHelperService = kafkaHelperService;
    }

    @PostMapping("/publish")
    public ResponseEntity<?> publish(@RequestBody KafkaPublishRequest req) {
        try {
            kafkaHelperService.publish(req);
            return ResponseEntity.ok(Map.of("published", true));
        } catch (Exception e) {
            log.error("Kafka publish failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/consume")
    public ResponseEntity<?> consume(@RequestBody KafkaConsumeRequest req) {
        try {
            ConsumeResponse response = kafkaHelperService.consume(req);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Kafka consume failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
