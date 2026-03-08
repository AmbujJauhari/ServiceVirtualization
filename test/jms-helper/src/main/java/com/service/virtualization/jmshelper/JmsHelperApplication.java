package com.service.virtualization.jmshelper;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jms.JmsAutoConfiguration;
import org.springframework.boot.autoconfigure.jms.activemq.ActiveMQAutoConfiguration;

/**
 * Standalone Spring Boot service for JMS publish/consume operations.
 *
 * Exposes simple HTTP endpoints used by Playwright E2E tests to drive
 * the full stub round-trip for IBM MQ and Tibco EMS without adding any
 * test-only code to the main backend.
 *
 * Run before the E2E test suite:
 *   cd test/jms-helper && mvn spring-boot:run
 *
 * Listens on port 9999 (configurable via JMS_HELPER_PORT env var).
 */
@SpringBootApplication(exclude = {
        JmsAutoConfiguration.class,
        ActiveMQAutoConfiguration.class
})
public class JmsHelperApplication {

    public static void main(String[] args) {
        SpringApplication.run(JmsHelperApplication.class, args);
    }
}
