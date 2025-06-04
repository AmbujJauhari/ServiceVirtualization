#!/usr/bin/env python3
"""
Standalone IBM MQ Publisher for TEST.QUEUE.1
Simple script to publish test messages without bringing up the Spring Boot application
"""

import pymqi
import json
import uuid
from datetime import datetime
import sys

# IBM MQ Connection Configuration
MQ_HOST = 'localhost'
MQ_PORT = 1414
MQ_QUEUE_MANAGER = 'QM1'
MQ_CHANNEL = 'DEV.APP.SVRCONN'
MQ_QUEUE_NAME = 'TEST.QUEUE.1'

def create_connection():
    """Create connection to IBM MQ"""
    try:
        print(f"🔗 Connecting to IBM MQ at {MQ_HOST}:{MQ_PORT}")
        print(f"🎯 Queue Manager: {MQ_QUEUE_MANAGER}")
        print(f"📡 Channel: {MQ_CHANNEL}")
        
        # Create connection descriptor
        cd = pymqi.CD()
        cd.ChannelName = MQ_CHANNEL
        cd.ConnectionName = f"{MQ_HOST}({MQ_PORT})"
        cd.ChannelType = pymqi.CMQC.MQCHT_CLNTCONN
        cd.TransportType = pymqi.CMQC.MQXPT_TCP
        
        # Connect to queue manager
        qmgr = pymqi.connect(MQ_QUEUE_MANAGER, cd)
        print("✅ Connected to IBM MQ successfully!")
        return qmgr
        
    except pymqi.MQMIError as e:
        print(f"❌ MQ Error: {e}")
        raise
    except Exception as e:
        print(f"❌ Connection Error: {e}")
        raise

def publish_message(qmgr, message_content, message_type="TEXT", properties=None):
    """Publish a message to the queue"""
    try:
        # Open queue for output
        q = pymqi.Queue(qmgr, MQ_QUEUE_NAME)
        
        # Create message descriptor
        md = pymqi.MD()
        md.Format = pymqi.CMQC.MQFMT_STRING
        
        # Set correlation ID if provided in properties
        if properties and 'correlation_id' in properties:
            md.CorrelId = properties['correlation_id'].encode()
        
        # Put message options
        pmo = pymqi.PMO()
        pmo.Options = pymqi.CMQC.MQPMO_NEW_MSG_ID
        
        # Send the message
        q.put(message_content.encode('utf-8'), md, pmo)
        
        message_id = md.MsgId.hex()
        print(f"📤 Published {message_type} message - ID: {message_id}")
        
        # Close queue
        q.close()
        return message_id
        
    except pymqi.MQMIError as e:
        print(f"❌ Failed to publish message: {e}")
        raise

def publish_json_message(qmgr):
    """Publish a JSON test message"""
    message_id = str(uuid.uuid4())
    
    json_message = {
        "messageId": message_id,
        "timestamp": datetime.now().isoformat(),
        "content": "Standalone Python publisher test message",
        "type": "JSON",
        "source": "PythonStandalonePublisher",
        "priority": "HIGH"
    }
    
    message_content = json.dumps(json_message, indent=2)
    properties = {"correlation_id": message_id}
    
    publish_message(qmgr, message_content, "JSON", properties)
    print(f"   📄 JSON Content: {json.dumps(json_message, indent=4)}")
    return message_id

def publish_xml_message(qmgr):
    """Publish an XML test message"""
    message_id = str(uuid.uuid4())
    
    xml_message = f"""<?xml version="1.0" encoding="UTF-8"?>
<testMessage>
    <messageId>{message_id}</messageId>
    <timestamp>{datetime.now().isoformat()}</timestamp>
    <content>Standalone Python XML test message</content>
    <type>XML</type>
    <source>PythonStandalonePublisher</source>
    <priority>MEDIUM</priority>
</testMessage>"""
    
    properties = {"correlation_id": message_id}
    
    publish_message(qmgr, xml_message, "XML", properties)
    print(f"   📄 XML Content:\n{xml_message}")
    return message_id

def publish_multiple_messages(qmgr, count=3):
    """Publish multiple text messages"""
    message_ids = []
    
    for i in range(1, count + 1):
        message_id = f"BATCH-{uuid.uuid4().hex[:8]}"
        message_content = f"Batch message #{i} - Test message from Python standalone publisher - ID: {message_id}"
        
        properties = {"correlation_id": message_id}
        
        publish_message(qmgr, message_content, "TEXT", properties)
        print(f"   📝 Message {i}/{count}: {message_content}")
        message_ids.append(message_id)
    
    return message_ids

def main():
    """Main function to run the publisher"""
    print("🚀 IBM MQ Standalone Publisher (Python)")
    print("📋 Target Queue: " + MQ_QUEUE_NAME)
    print("=" * 60)
    
    qmgr = None
    try:
        # Connect to IBM MQ
        qmgr = create_connection()
        
        # Publish different types of messages
        print("\n1. Publishing JSON message...")
        publish_json_message(qmgr)
        
        print("\n2. Publishing XML message...")
        publish_xml_message(qmgr)
        
        print("\n3. Publishing multiple text messages...")
        publish_multiple_messages(qmgr, 3)
        
        print("\n" + "=" * 60)
        print("✅ All messages published successfully to TEST.QUEUE.1!")
        
    except Exception as e:
        print(f"\n❌ Publisher failed: {e}")
        sys.exit(1)
        
    finally:
        if qmgr:
            qmgr.disconnect()
            print("🔌 Disconnected from IBM MQ")

if __name__ == "__main__":
    try:
        import pymqi
    except ImportError:
        print("❌ pymqi library not found. Install with: pip install pymqi")
        print("📝 Note: You may need to install IBM MQ client libraries first")
        sys.exit(1)
    
    main() 