package com.service.virtualization.jmshelper.controller;

import com.service.virtualization.jmshelper.model.ConsumeRequest;
import com.service.virtualization.jmshelper.model.ConsumeResponse;
import com.service.virtualization.jmshelper.model.PublishRequest;
import com.service.virtualization.jmshelper.service.TibcoJmsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * HTTP endpoints for Tibco EMS message publish / consume.
 *
 * POST /tibco/publish  — publish a message to a queue or topic
 * POST /tibco/consume  — synchronously consume one message (blocks up to timeoutMs)
 */
@RestController
@RequestMapping("/tibco")
public class TibcoController {

    private static final Logger log = LoggerFactory.getLogger(TibcoController.class);

    private final TibcoJmsService tibcoJmsService;

    public TibcoController(TibcoJmsService tibcoJmsService) {
        this.tibcoJmsService = tibcoJmsService;
    }

    @PostMapping("/publish")
    public ResponseEntity<?> publish(@RequestBody PublishRequest req) {
        try {
            tibcoJmsService.publish(req);
            return ResponseEntity.ok(Map.of("published", true));
        } catch (Exception e) {
            log.error("Tibco publish failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/consume")
    public ResponseEntity<?> consume(@RequestBody ConsumeRequest req) {
        try {
            ConsumeResponse response = tibcoJmsService.consume(req);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Tibco consume failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
