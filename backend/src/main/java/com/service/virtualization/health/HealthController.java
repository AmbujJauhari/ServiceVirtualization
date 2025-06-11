package com.service.virtualization.health;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.HealthContributorRegistry;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    @Autowired
    private HealthContributorRegistry healthContributorRegistry;
    
    @Autowired
    private Environment environment;

    @GetMapping("/status")
    public Map<String, Object> getHealthStatus() {
        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> services = new ArrayList<>();
        AtomicReference<String> overallStatus = new AtomicReference<>("UP");

        // Application health
        Map<String, Object> applicationHealth = new HashMap<>();
        applicationHealth.put("name", "Application");
        applicationHealth.put("status", "UP");
        applicationHealth.put("lastChecked", LocalDateTime.now().toInstant(ZoneOffset.UTC).toString());
        
        // Get all health contributors from Spring Boot Actuator
        healthContributorRegistry.stream()
            .filter(entry -> entry.getContributor() instanceof HealthIndicator)
            .forEach(entry -> {
                String name = entry.getName();
                HealthIndicator indicator = (HealthIndicator) entry.getContributor();
                
                // Skip internal indicators that don't represent external services
                if (name.equals("ping") || name.equals("diskSpace")) {
                    return;
                }
                
                try {
                    org.springframework.boot.actuate.health.Health health = indicator.health();
                    Status status = health.getStatus();
                    
                    Map<String, Object> serviceHealth = new HashMap<>();
                    serviceHealth.put("name", formatServiceName(name));
                    serviceHealth.put("status", mapStatus(status));
                    serviceHealth.put("lastChecked", LocalDateTime.now().toInstant(ZoneOffset.UTC).toString());
                    
                    // Include details if available and status is not UP
                    if (!Status.UP.equals(status) && health.getDetails() != null && !health.getDetails().isEmpty()) {
                        serviceHealth.put("details", health.getDetails().toString());
                    }
                    
                    services.add(serviceHealth);
                    
                    // Update overall status based on service health
                    if (Status.DOWN.equals(status) && !"DOWN".equals(overallStatus.get())) {
                        overallStatus.set("DOWN");
                    } else if (Status.UNKNOWN.equals(status) && "UP".equals(overallStatus.get())) {
                        overallStatus.set("DEGRADED");
                    }
                } catch (Exception e) {
                    Map<String, Object> serviceHealth = new HashMap<>();
                    serviceHealth.put("name", formatServiceName(name));
                    serviceHealth.put("status", "DOWN");
                    serviceHealth.put("lastChecked", LocalDateTime.now().toInstant(ZoneOffset.UTC).toString());
                    serviceHealth.put("details", "Error checking health: " + e.getMessage());
                    
                    services.add(serviceHealth);
                    overallStatus.set("DOWN");
                }
            });
        
        response.put("overall", overallStatus.get());
        response.put("application", applicationHealth);
        response.put("services", services);
        
        return response;
    }
    
    @GetMapping("/protocols")
    public Map<String, Object> getProtocolStatus() {
        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> protocols = new ArrayList<>();
        
        // Check each protocol based on active profiles
        String[] activeProfiles = environment.getActiveProfiles();
        List<String> activeProfilesList = List.of(activeProfiles);
        
        // Kafka
        Map<String, Object> kafka = new HashMap<>();
        kafka.put("name", "Kafka");
        kafka.put("enabled", !activeProfilesList.contains("kafka-disabled"));
        kafka.put("reason", activeProfilesList.contains("kafka-disabled") ? "Disabled by kafka-disabled profile" : "Enabled by default");
        protocols.add(kafka);
        
        // ActiveMQ
        Map<String, Object> activemq = new HashMap<>();
        activemq.put("name", "ActiveMQ");
        activemq.put("enabled", !activeProfilesList.contains("activemq-disabled"));
        activemq.put("reason", activeProfilesList.contains("activemq-disabled") ? "Disabled by activemq-disabled profile" : "Enabled by default");
        protocols.add(activemq);
        
        // IBM MQ
        Map<String, Object> ibmmq = new HashMap<>();
        ibmmq.put("name", "IBM MQ");
        ibmmq.put("enabled", !activeProfilesList.contains("ibmmq-disabled"));
        ibmmq.put("reason", activeProfilesList.contains("ibmmq-disabled") ? "Disabled by ibmmq-disabled profile" : "Enabled by default");
        protocols.add(ibmmq);
        
        // TIBCO EMS
        Map<String, Object> tibco = new HashMap<>();
        tibco.put("name", "TIBCO EMS");
        tibco.put("enabled", !activeProfilesList.contains("tibco-disabled"));
        tibco.put("reason", activeProfilesList.contains("tibco-disabled") ? "Disabled by tibco-disabled profile" : "Enabled by default");
        protocols.add(tibco);
        
        // REST
        Map<String, Object> rest = new HashMap<>();
        rest.put("name", "REST");
        rest.put("enabled", !activeProfilesList.contains("rest-disabled"));
        rest.put("reason", activeProfilesList.contains("rest-disabled") ? "Disabled by rest-disabled profile" : "Enabled by default");
        protocols.add(rest);
        
        // SOAP
        Map<String, Object> soap = new HashMap<>();
        soap.put("name", "SOAP");
        soap.put("enabled", !activeProfilesList.contains("soap-disabled"));
        soap.put("reason", activeProfilesList.contains("soap-disabled") ? "Disabled by soap-disabled profile" : "Enabled by default");
        protocols.add(soap);
        
        response.put("protocols", protocols);
        response.put("timestamp", LocalDateTime.now().toInstant(ZoneOffset.UTC).toString());
        
        return response;
    }
    
    private String formatServiceName(String name) {
        // Convert camelCase or kebab-case to a more readable format
        name = name.replaceAll("([a-z])([A-Z])", "$1 $2")
                  .replaceAll("-", " ");
        
        // Special case handling
        switch (name.toLowerCase()) {
            case "db":
            case "mongo":
            case "mongodb":
                return "MongoDB";
            case "kafka":
                return "Kafka";
            case "ibm mq":
            case "ibmmq":
                return "IBM MQ";
            case "tibco":
            case "tibco ems":
                return "TIBCO EMS";
            case "active mq":
            case "activemq":
                return "ActiveMQ";
            default:
                // Capitalize first letter of each word
                return name.substring(0, 1).toUpperCase() + name.substring(1);
        }
    }
    
    private String mapStatus(Status status) {
        if (Status.UP.equals(status)) {
            return "UP";
        } else if (Status.DOWN.equals(status)) {
            return "DOWN";
        } else {
            return "DEGRADED";
        }
    }
} 