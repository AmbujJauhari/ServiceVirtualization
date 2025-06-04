package com.service.virtualization.ibmmq;

import com.ibm.mq.MQEnvironment;
import com.ibm.mq.MQQueueManager;
import com.ibm.mq.constants.MQConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Hashtable;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test IBM MQ connection with simplified configuration (no authentication required)
 */
public class IBMMQConnectionTest {

    @Test
    @DisplayName("Test IBM MQ connection with basic parameters only")
    public void testConnectionWithBasicParameters() throws Exception {
        // Set up connection properties - only basic parameters needed
        Hashtable<String, Object> props = new Hashtable<>();
        props.put(MQConstants.HOST_NAME_PROPERTY, "localhost");
        props.put(MQConstants.PORT_PROPERTY, 1414);
        props.put(MQConstants.CHANNEL_PROPERTY, "DEV.APP.SVRCONN");
        props.put(MQConstants.TRANSPORT_PROPERTY, MQConstants.TRANSPORT_MQSERIES_CLIENT);
        
        // Set connection properties
        MQEnvironment.properties = props;
        
        // Test connection
        MQQueueManager queueManager = null;
        try {
            queueManager = new MQQueueManager("QM1");
            
            // If we get here, connection was successful
            assertNotNull(queueManager);
            assertTrue(queueManager.isConnected());
            
            System.out.println("✅ Successfully connected to IBM MQ with basic parameters:");
            System.out.println("   - Host: localhost");
            System.out.println("   - Port: 1414");
            System.out.println("   - Queue Manager: QM1");
            System.out.println("   - Channel: DEV.APP.SVRCONN");
            System.out.println("   - Transport Type: CLIENT");
            System.out.println("   - Authentication: NONE (as required)");
            
        } finally {
            if (queueManager != null && queueManager.isConnected()) {
                queueManager.disconnect();
                System.out.println("✅ Disconnected from IBM MQ successfully");
            }
        }
    }
    
    @Test
    @DisplayName("Test queue existence verification")
    public void testQueueExistence() throws Exception {
        // Set up connection properties
        Hashtable<String, Object> props = new Hashtable<>();
        props.put(MQConstants.HOST_NAME_PROPERTY, "localhost");
        props.put(MQConstants.PORT_PROPERTY, 1414);
        props.put(MQConstants.CHANNEL_PROPERTY, "DEV.APP.SVRCONN");
        props.put(MQConstants.TRANSPORT_PROPERTY, MQConstants.TRANSPORT_MQSERIES_CLIENT);
        
        MQEnvironment.properties = props;
        
        MQQueueManager queueManager = null;
        try {
            queueManager = new MQQueueManager("QM1");
            
            // Test accessing a created queue
            String testQueueName = "DEV.QUEUE.1";
            
            // Try to open the queue (this will fail if queue doesn't exist)
            var queue = queueManager.accessQueue(testQueueName, MQConstants.MQOO_INQUIRE);
            assertNotNull(queue);
            
            System.out.println("✅ Successfully accessed queue: " + testQueueName);
            
            queue.close();
            
        } finally {
            if (queueManager != null && queueManager.isConnected()) {
                queueManager.disconnect();
            }
        }
    }
} 