package com.service.virtualization.jmshelper.controller;

import com.service.virtualization.jmshelper.model.ConsumeRequest;
import com.service.virtualization.jmshelper.model.ConsumeResponse;
import com.service.virtualization.jmshelper.model.PublishRequest;
import com.service.virtualization.jmshelper.service.IbmmqJmsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * HTTP endpoints for IBM MQ message publish / consume.
 *
 * POST /ibmmq/publish  — publish a message to a queue or topic
 * POST /ibmmq/consume  — synchronously consume one message (blocks up to timeoutMs)
 */
@RestController
@RequestMapping("/ibmmq")
public class IbmmqController {

    private static final Logger log = LoggerFactory.getLogger(IbmmqController.class);

    private final IbmmqJmsService ibmmqJmsService;

    public IbmmqController(IbmmqJmsService ibmmqJmsService) {
        this.ibmmqJmsService = ibmmqJmsService;
    }

    @PostMapping("/publish")
    public ResponseEntity<?> publish(@RequestBody PublishRequest req) {
        try {
            ibmmqJmsService.publish(req);
            return ResponseEntity.ok(Map.of("published", true));
        } catch (Exception e) {
            log.error("IBM MQ publish failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/consume")
    public ResponseEntity<?> consume(@RequestBody ConsumeRequest req) {
        try {
            ConsumeResponse response = ibmmqJmsService.consume(req);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("IBM MQ consume failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
