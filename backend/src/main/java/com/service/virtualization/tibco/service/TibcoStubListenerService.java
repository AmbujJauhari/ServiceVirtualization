package com.service.virtualization.tibco.service;

import com.service.virtualization.tibco.model.TibcoDestination;
import com.service.virtualization.tibco.model.TibcoStub;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.jms.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Service responsible for managing TIBCO EMS listeners for stubs.
 * This service creates message consumers for each active stub and
 * sends responses based on matching criteria.
 */
@Service
@Profile("tibco")
public class TibcoStubListenerService {
    private static final Logger logger = LoggerFactory.getLogger(TibcoStubListenerService.class);

    private final TibcoStubService tibcoStubService;
    private final ConnectionFactory connectionFactory;
    private final Map<String, StubListener> activeListeners = new ConcurrentHashMap<>();

    @Autowired
    public TibcoStubListenerService(
            TibcoStubService tibcoStubService,
            @Qualifier("tibcoConnectionFactory") ConnectionFactory connectionFactory) {
        this.tibcoStubService = tibcoStubService;
        this.connectionFactory = connectionFactory;
    }

    @PostConstruct
    public void init() {
        refreshListeners();
    }

    @PreDestroy
    public void cleanup() {
        // Shutdown all active listeners
        for (StubListener listener : activeListeners.values()) {
            listener.close();
        }
        activeListeners.clear();
    }

    /**
     * Refreshes all active listeners based on active stubs.
     * This should be called when stubs are created, updated, or deleted.
     */
    public synchronized void refreshListeners() {
        logger.info("Refreshing TIBCO stub listeners");
        
        // Get all active stubs
        List<TibcoStub> activeStubs = tibcoStubService.findAllByStatus("ACTIVE");
        
        // Track stubs that need listeners
        Set<String> currentStubIds = new HashSet<>();
        
        // Create or update listeners for active stubs
        for (TibcoStub stub : activeStubs) {
            currentStubIds.add(stub.getId());
            
            // Check if listener already exists
            if (activeListeners.containsKey(stub.getId())) {
                StubListener existingListener = activeListeners.get(stub.getId());
                // If stub has changed, close the existing listener and create a new one
                if (existingListener.isStubChanged(stub)) {
                    logger.info("Stub {} has changed, recreating listener", stub.getId());
                    existingListener.close();
                    createListener(stub);
                }
            } else {
                // Create new listener
                createListener(stub);
            }
        }
        
        // Remove listeners for deleted or inactive stubs
        List<String> listenersToRemove = new ArrayList<>();
        for (String stubId : activeListeners.keySet()) {
            if (!currentStubIds.contains(stubId)) {
                listenersToRemove.add(stubId);
            }
        }
        
        for (String stubId : listenersToRemove) {
            logger.info("Removing listener for stub {}", stubId);
            StubListener listener = activeListeners.remove(stubId);
            listener.close();
        }
        
        logger.info("TIBCO stub listeners refreshed: {} active listeners", activeListeners.size());
    }
    
    /**
     * Creates a new stub listener for the given stub.
     */
    private void createListener(TibcoStub stub) {
        try {
            StubListener listener = new StubListener(stub);
            activeListeners.put(stub.getId(), listener);
            listener.start();
            logger.info("Created listener for stub {}: {}", stub.getId(), stub.getName());
        } catch (Exception e) {
            logger.error("Failed to create listener for stub {}: {}", stub.getId(), e.getMessage(), e);
        }
    }
    
    /**
     * Class representing a listener for a single stub.
     */
    private class StubListener {
        private final TibcoStub stub;
        private Connection connection;
        private Session session;
        private MessageConsumer consumer;
        private MessageProducer producer;
        private final String messageSelector;
        private Destination requestDestination;
        private Destination responseDestination;

        public StubListener(TibcoStub stub) {
            this.stub = stub;
            this.messageSelector = stub.getMessageSelector();
        }
        
        /**
         * Checks if the stub has changed since this listener was created.
         */
        public boolean isStubChanged(TibcoStub newStub) {
            // Check if any relevant fields changed that would require recreating the listener
            return !Objects.equals(stub.getRequestDestination().getType(), newStub.getRequestDestination().getType()) ||
                   !Objects.equals(stub.getRequestDestination().getName(), newStub.getRequestDestination().getName()) ||
                   !Objects.equals(stub.getResponseDestination().getType(), newStub.getResponseDestination().getType()) ||
                   !Objects.equals(stub.getResponseDestination().getName(), newStub.getResponseDestination().getName()) ||
                   !Objects.equals(stub.getMessageSelector(), newStub.getMessageSelector());
        }
        
        /**
         * Starts the listener.
         */
        public void start() throws JMSException {
            try {
                // Create connection
                connection = connectionFactory.createConnection();
                connection.setClientID("ServiceVirtualization_" + stub.getId());
                session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                
                // Create request destination
                TibcoDestination requestDestConfig = stub.getRequestDestination();
                requestDestination = createDestination(session, requestDestConfig);
                
                // Create response destination
                TibcoDestination responseDestConfig = stub.getResponseDestination();
                responseDestination = createDestination(session, responseDestConfig);
                
                // Create producer for response destination
                producer = session.createProducer(responseDestination);
                
                // Create consumer with message selector if specified
                if (messageSelector != null && !messageSelector.isEmpty()) {
                    consumer = session.createConsumer(requestDestination, messageSelector);
                } else {
                    consumer = session.createConsumer(requestDestination);
                }
                
                // Register message listener
                consumer.setMessageListener(this::onMessage);
                
                // Start the connection
                connection.start();
                
                logger.info("Started listener for stub {}: {} -> {}", 
                        stub.getId(), 
                        requestDestConfig.getName(), 
                        responseDestConfig.getName());
            } catch (JMSException e) {
                close();
                throw e;
            }
        }
        
        /**
         * Creates a JMS destination based on the configured destination.
         */
        private Destination createDestination(Session session, TibcoDestination destConfig) throws JMSException {
            if ("TOPIC".equals(destConfig.getType())) {
                return session.createTopic(destConfig.getName());
            } else if ("QUEUE".equals(destConfig.getType())) {
                return session.createQueue(destConfig.getName());
            } else {
                throw new IllegalArgumentException("Unsupported destination type: " + destConfig.getType());
            }
        }
        
        /**
         * Handles incoming messages.
         */
        private void onMessage(Message message) {
            try {
                logger.debug("Received message for stub {}: {}", stub.getId(), stub.getName());
                
                // Check if body match criteria exist and need to be validated
                if (stub.getBodyMatchCriteria() != null && !stub.getBodyMatchCriteria().isEmpty()) {
                    // Extract message body
                    String messageBody = extractMessageBody(message);
                    if (messageBody == null || !matchesBodyCriteria(messageBody)) {
                        logger.debug("Message body doesn't match criteria for stub {}", stub.getId());
                        return; // Skip this message as it doesn't match body criteria
                    }
                }
                
                // Message matches, process it
                // Check if we're using direct response or callback
                if ("direct".equalsIgnoreCase(stub.getResponseType())) {
                    sendDirectResponse(message);
                } else if ("callback".equalsIgnoreCase(stub.getResponseType())) {
                    forwardToCallback(message);
                }
            } catch (Exception e) {
                logger.error("Error processing message for stub {}: {}", stub.getId(), e.getMessage(), e);
            }
        }
        
        /**
         * Extracts the message body as a string.
         */
        private String extractMessageBody(Message message) throws JMSException {
            if (message instanceof TextMessage) {
                return ((TextMessage) message).getText();
            } else if (message instanceof BytesMessage) {
                BytesMessage bytesMessage = (BytesMessage) message;
                byte[] bytes = new byte[(int) bytesMessage.getBodyLength()];
                bytesMessage.readBytes(bytes);
                return new String(bytes);
            } else if (message instanceof MapMessage) {
                // For MapMessage, you might want to convert to JSON
                MapMessage mapMessage = (MapMessage) message;
                StringBuilder json = new StringBuilder("{");
                Enumeration<?> mapNames = mapMessage.getMapNames();
                while (mapNames.hasMoreElements()) {
                    String name = (String) mapNames.nextElement();
                    Object value = mapMessage.getObject(name);
                    json.append("\"").append(name).append("\":\"").append(value).append("\"");
                    if (mapNames.hasMoreElements()) {
                        json.append(",");
                    }
                }
                json.append("}");
                return json.toString();
            }
            return null;
        }
        
        /**
         * Checks if the message body matches all the criteria.
         */
        private boolean matchesBodyCriteria(String messageBody) {
            for (TibcoStub.BodyMatchCriteria criteria : stub.getBodyMatchCriteria()) {
                if (!matchesSingleCriteria(messageBody, criteria)) {
                    return false;
                }
            }
            return true;
        }
        
        /**
         * Checks if the message body matches a single criterion.
         */
        private boolean matchesSingleCriteria(String messageBody, TibcoStub.BodyMatchCriteria criteria) {
            try {
                String extractedValue;
                
                // Extract value using the appropriate filePath expression
                if ("xpath".equalsIgnoreCase(criteria.getType())) {
                    extractedValue = evaluateXPath(messageBody, criteria.getExpression());
                } else if ("jsonpath".equalsIgnoreCase(criteria.getType())) {
                    extractedValue = evaluateJsonPath(messageBody, criteria.getExpression());
                } else {
                    logger.warn("Unsupported match type: {}", criteria.getType());
                    return false;
                }
                
                if (extractedValue == null) {
                    logger.debug("Could not extract value using {} expression: {}", 
                            criteria.getType(), criteria.getExpression());
                    return false;
                }
                
                // Compare the extracted value with the expected value
                return compareValues(extractedValue, criteria.getValue(), criteria.getOperator());
                
            } catch (Exception e) {
                logger.warn("Error evaluating body match criteria: {}", e.getMessage());
                return false;
            }
        }
        
        /**
         * Evaluates an XPath expression against an XML string.
         */
        private String evaluateXPath(String xml, String xpath) throws Exception {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true); // Required for XPath
            Document doc = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
            
            XPath xPath = XPathFactory.newInstance().newXPath();
            XPathExpression expr = xPath.compile(xpath);
            return expr.evaluate(doc);
        }
        
        /**
         * Evaluates a JSONPath expression against a JSON string.
         */
        private String evaluateJsonPath(String json, String jsonPath) {
            try {
                Object result = JsonPath.read(json, jsonPath);
                return result != null ? result.toString() : null;
            } catch (PathNotFoundException e) {
                return null;
            }
        }
        
        /**
         * Compares two string values using the specified operator.
         */
        private boolean compareValues(String actual, String expected, String operator) {
            if (actual == null) {
                return expected == null;
            }
            
            switch (operator.toLowerCase()) {
                case "equals":
                    return actual.equals(expected);
                case "contains":
                    return actual.contains(expected);
                case "startswith":
                    return actual.startsWith(expected);
                case "endswith":
                    return actual.endsWith(expected);
                case "regex":
                    Pattern pattern = Pattern.compile(expected);
                    Matcher matcher = pattern.matcher(actual);
                    return matcher.matches();
                default:
                    logger.warn("Unsupported operator: {}", operator);
                    return false;
            }
        }
        
        /**
         * Sends a direct response based on the stub configuration.
         */
        private void sendDirectResponse(Message requestMessage) throws JMSException {
            // Create response message
            TextMessage responseMessage = session.createTextMessage(stub.getResponseContent());
            
            // Copy message ID for correlation
            String requestMessageId = requestMessage.getJMSMessageID();
            responseMessage.setJMSCorrelationID(requestMessageId);
            
            // Set headers if specified
            if (stub.getResponseHeaders() != null) {
                for (Map.Entry<String, String> header : stub.getResponseHeaders().entrySet()) {
                    responseMessage.setStringProperty(header.getKey(), header.getValue());
                }
            }
            
            // Send response
            producer.send(responseMessage);
            logger.debug("Sent response for stub {}: {}", stub.getId(), stub.getName());
        }
        
        /**
         * Forwards the message to a callback URL.
         */
        private void forwardToCallback(Message message) {
            // This would be implemented with HTTP client to forward to callback URL
            logger.info("Callback not yet implemented for stub {}", stub.getId());
        }
        
        /**
         * Closes the listener and releases resources.
         */
        public void close() {
            try {
                if (consumer != null) {
                    consumer.close();
                }
                if (producer != null) {
                    producer.close();
                }
                if (session != null) {
                    session.close();
                }
                if (connection != null) {
                    connection.close();
                }
                logger.debug("Closed listener for stub {}", stub.getId());
            } catch (JMSException e) {
                logger.error("Error closing listener for stub {}: {}", stub.getId(), e.getMessage(), e);
            }
        }
    }
} 