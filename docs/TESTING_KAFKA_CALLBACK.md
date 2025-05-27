# Testing Kafka Callback Functionality

## ✅ **Refactored KafkaWebhookTestController**

The `KafkaWebhookTestController` has been refactored to use the proper `KafkaCallbackService.CallbackResponse` structure, ensuring type safety and consistency.

## 🧪 **Available Test Endpoints**

### **1. Order Processing Webhook**
```http
POST http://localhost:8080/webhook/test/process-order
```
**Response Type:** `KafkaCallbackService.CallbackResponse`
```json
{
  "key": "order-response-1640995200000",
  "responseContent": "{\"orderId\": \"ORD-1640995200000\", \"status\": \"PROCESSED\", \"message\": \"Order has been successfully processed\", \"originalRequest\": \"...\", \"processedAt\": \"1640995200000\"}",
  "responseFormat": "JSON",
  "responseTopic": "test-orders-orders-processed"
}
```

### **2. Payment Processing Webhook**
```http
POST http://localhost:8080/webhook/test/process-payment
```
**Response Type:** `KafkaCallbackService.CallbackResponse`
```json
{
  "key": "payment-response-1640995200000",
  "responseContent": "{\"paymentId\": \"PAY-1640995200000\", \"status\": \"APPROVED\", \"amount\": 100.00, \"currency\": \"USD\", \"message\": \"Payment processed successfully\", \"originalRequest\": \"...\", \"processedAt\": \"1640995200000\"}",
  "responseFormat": "JSON",
  "responseTopic": "test-orders-payments-processed"
}
```

### **3. Echo Webhook**
```http
POST http://localhost:8080/webhook/test/echo
```
**Response Type:** `KafkaCallbackService.CallbackResponse`
```json
{
  "key": "echo-1640995200000",
  "responseContent": "{\"echo\": \"original message\", \"processed_at\": \"1640995200000\"}",
  "responseFormat": "JSON",
  "responseTopic": null
}
```
*Note: No custom topic - will use stub configuration or auto-generated topic.*

### **4. AVRO Response Webhook**
```http
POST http://localhost:8080/webhook/test/avro-response
```
**Response Type:** `KafkaCallbackService.CallbackResponse`
```json
{
  "key": "avro-response-1640995200000",
  "responseContent": "{\"namespace\": \"com.example.avro\", \"type\": \"record\", \"name\": \"ProcessedMessage\", \"fields\": [...]}",
  "responseFormat": "AVRO",
  "responseTopic": "test-orders-avro-processed"
}
```

### **5. No Key Webhook (Tests Auto-Generation)**
```http
POST http://localhost:8080/webhook/test/no-key
```
**Response Type:** `KafkaCallbackService.CallbackResponse`
```json
{
  "key": null,
  "responseContent": "{\"message\": \"Response without key\", \"original\": \"original message\"}",
  "responseFormat": "JSON",
  "responseTopic": null
}
```
*Note: Both `key` and `responseTopic` are null, triggering auto-generation.*

### **6. Topic Router Webhook (NEW)**
```http
POST http://localhost:8080/webhook/test/topic-router
```
**Response Type:** `KafkaCallbackService.CallbackResponse`
Routes messages to different topics based on content:
- Messages containing "error" or "failed" → `{requestTopic}-errors`
- Messages containing "urgent" or "priority" → `{requestTopic}-priority`
- All other messages → `{requestTopic}-standard`

**Example Response:**
```json
{
  "key": "routed-1640995200000",
  "responseContent": "{\"status\": \"PRIORITY\", \"message\": \"Urgent processing completed\", \"original\": \"urgent order\"}",
  "responseFormat": "JSON",
  "responseTopic": "test-orders-priority"
}
```

### **7. Error Webhook**
```http
POST http://localhost:8080/webhook/test/error
```
**HTTP Status:** `500 Internal Server Error`
```json
{
  "error": "Simulated webhook error"
}
```

### **8. Slow Webhook (3-second delay)**
```http
POST http://localhost:8080/webhook/test/slow
```
**Response Type:** `KafkaCallbackService.CallbackResponse` (after 3-second delay)
```json
{
  "key": "slow-response-1640995200000",
  "responseContent": "{\"status\": \"processed after delay\", \"delay_ms\": 3000}",
  "responseFormat": "JSON"
}
```

## 🚀 **Step-by-Step Testing Process**

### **Step 1: Start the Backend**
```bash
cd backend
mvn spring-boot:run
```

### **Step 2: Create a Kafka Stub**
```http
POST http://localhost:8080/kafka/stubs
Content-Type: application/json

{
  "name": "test-order-callback",
  "description": "Test order processing callback",
  "userId": "test-user",
  "requestTopic": "test-orders",
  "responseTopic": "test-orders-response",
  "responseKey": "order-response-key",
  "responseType": "callback",
  "callbackUrl": "http://localhost:8080/webhook/test/process-order",
  "callbackHeaders": {
    "Authorization": "Bearer test-token"
  },
  "keyPattern": ".*",
  "valuePattern": ".*",
  "latency": 0,
  "status": "active"
}
```

### **Direct Response Example**
For direct responses, you can also specify a responseKey:
```http
POST http://localhost:8080/kafka/stubs
Content-Type: application/json

{
  "name": "test-direct-response",
  "description": "Test direct response with custom key",
  "userId": "test-user",
  "requestTopic": "test-orders",
  "responseTopic": "test-orders-response",
  "responseKey": "direct-response-123",
  "responseType": "direct",
  "responseContent": "{\"status\": \"processed\", \"orderId\": \"{{orderId}}\"}",
  "responseContentFormat": "JSON",
  "latency": 0,
  "status": "active"
}
```

### **Step 3: Send Test Message to Kafka**
You can use Kafka tools or the publisher endpoint:
```http
POST http://localhost:8080/kafka/publish
Content-Type: application/json

{
  "topic": "test-orders",
  "key": "order-123",
  "message": "{\"orderId\": 123, \"amount\": 99.99, \"customerId\": \"cust-456\"}",
  "headers": {}
}
```

### **Step 4: Verify Response in Kafka Topic**
Check the `test-orders-response` topic for the processed message:
- **Key:** `order-response-{timestamp}`
- **Content:** Processed order JSON with status "PROCESSED"
- **Headers:** Include `content-format`, `source-stub-id`, `response-type`, etc.

## 🔍 **Expected Flow Verification**

1. **Message Published:** `test-orders` topic receives the order message
2. **Stub Matched:** The listener finds the active stub for `test-orders`
3. **Webhook Called:** HTTP POST to `http://localhost:8080/webhook/test/process-order`
4. **Response Parsed:** `CallbackResponse` object with processed order data
5. **Kafka Published:** Response data published to `test-orders-response` topic

## 📊 **Logging to Watch**

Watch for these log messages:
```
🔗 Calling webhook for stub 'test-order-callback' at URL: http://localhost:8080/webhook/test/process-order
📥 Received webhook call for order processing: {...}
📤 Returning webhook response: key=order-response-..., format=JSON
✅ Webhook call successful for stub 'test-order-callback'. Status: 200
📨 Webhook response published to Kafka - Topic: test-orders-response, Key: order-response-..., Format: JSON
```

## 🧪 **Test Scenarios**

### **Scenario 1: Basic Order Processing**
- Stub: `/process-order` endpoint
- Expected: Order gets processed and published to custom topic `{requestTopic}-orders-processed`

### **Scenario 2: Auto-Key Generation**
- Stub: `/no-key` endpoint  
- Expected: UUID auto-generated for response key, default topic used

### **Scenario 3: Different Format Support**
- Stub: `/avro-response` endpoint
- Expected: Response published with `AVRO` format to `{requestTopic}-avro-processed`

### **Scenario 4: Dynamic Topic Routing**
- Stub: `/topic-router` endpoint
- Test with different message content:
  - "urgent order" → routes to `{requestTopic}-priority`
  - "failed transaction" → routes to `{requestTopic}-errors`
  - "regular order" → routes to `{requestTopic}-standard`

### **Scenario 5: Topic Priority Override**
- Create stub with `responseTopic: "stub-configured-topic"`
- Use webhook that returns `responseTopic: "webhook-override-topic"`
- Expected: Message published to `webhook-override-topic` (webhook wins)

### **Scenario 6: Error Handling**
- Stub: `/error` endpoint
- Expected: Error logged, no Kafka message published

### **Scenario 7: Latency Testing**
- Stub: `/slow` endpoint with latency in stub config
- Expected: Delayed webhook call, response to `{requestTopic}-slow-responses`

### **Scenario 8: Direct Response with Custom Key**
- Create stub with `responseType: "direct"` and `responseKey: "custom-key-123"`
- Send message to request topic
- Expected: Response published with key `custom-key-123`

### **Scenario 9: Direct Response with Auto-Generated Key**
- Create stub with `responseType: "direct"` and empty/null `responseKey`
- Send message to request topic
- Expected: Response published with auto-generated UUID key

## 🎯 **Key Benefits of Refactored Controller**

1. **Type Safety:** Uses `CallbackResponse` instead of generic `Map`
2. **Consistency:** Matches exactly what `KafkaCallbackService` expects
3. **Testing Variety:** Multiple endpoints for different scenarios
4. **Format Support:** Tests JSON, AVRO, and other formats
5. **Error Scenarios:** Includes error and timeout testing
6. **Auto-Generation:** Tests UUID auto-generation for keys

## 🔧 **Health Check**
```http
GET http://localhost:8080/webhook/test/health
```
**Response:**
```json
{
  "status": "UP",
  "service": "Kafka Webhook Test"
}
```

This ensures the webhook test controller is running and ready for testing!

## 📤 **Expected Webhook Response Format**

The webhook **MUST** return this structure:

```json
{
  "key": "response-key-789",
  "responseContent": "{\"orderId\": 456, \"status\": \"PROCESSED\", \"amount\": 100.00}",
  "responseFormat": "JSON",
  "responseTopic": "custom-response-topic"
}
```

### **Response Fields:**
- `key` *(optional)*: Kafka message key for response (auto-generated if not provided)
- `responseContent` *(required)*: The actual message content to publish
- `responseFormat` *(optional)*: Content format metadata (defaults to "JSON")
- `responseTopic` *(optional)*: Custom topic to publish response to (overrides stub configuration)

### **Topic Resolution Priority:**
1. **Webhook responseTopic** (highest priority)
2. **Stub configuration responseTopic**
3. **Auto-generated** `{requestTopic}-response` (fallback) 