# Kafka Callback Implementation - Dynamic Response Flow

## ✅ **Correct Implementation Overview**

The Kafka callback functionality now works as follows:

1. **Receive Kafka Message** → Match stub with `responseType: "callback"`
2. **Call Webhook URL** → Send original request data to external webhook
3. **Get Dynamic Response** → Webhook returns `{key, responseContent, responseFormat}`
4. **Publish to Kafka** → Use webhook response data to publish message to response topic

## 🔄 **Flow Diagram**

```
Kafka Request Message
        ↓
  Match Kafka Stub (responseType: "callback")
        ↓
  HTTP POST to callbackUrl
        ↓
  Webhook Returns: {key, responseContent, responseFormat}
        ↓
  Publish to Kafka Response Topic
```

## 📋 **Webhook Request Format**

The webhook receives this payload:

```json
{
  "stubId": "stub-123",
  "stubName": "order-processor",
  "request": {
    "topic": "orders",
    "key": "order-456",
    "message": "{\"orderId\": 456, \"amount\": 100.00}"
  },
  "timestamp": 1640995200000
}
```

## 📤 **Expected Webhook Response Format**

The webhook **MUST** return this structure:

```json
{
  "key": "response-key-789",
  "responseContent": "{\"orderId\": 456, \"status\": \"PROCESSED\", \"amount\": 100.00}",
  "responseFormat": "JSON"
}
```

### **Response Fields:**
- `key` *(optional)*: Kafka message key for response (auto-generated if not provided)
- `responseContent` *(required)*: The actual message content to publish
- `responseFormat` *(optional)*: Content format metadata (defaults to "JSON")

## 🧪 **Test Webhook Examples**

Use the provided test endpoints to understand the expected behavior:

### **Order Processing Webhook**
```http
POST http://localhost:8080/webhook/test/process-order
```

Returns:
```json
{
  "key": "order-response-1640995200000",
  "responseContent": "{\"orderId\": \"ORD-1640995200000\", \"status\": \"PROCESSED\", \"message\": \"Order has been successfully processed\"}",
  "responseFormat": "JSON"
}
```

### **Echo Webhook** 
```http
POST http://localhost:8080/webhook/test/echo
```

Returns:
```json
{
  "key": "echo-1640995200000",
  "responseContent": "{\"echo\": \"original message\", \"processed_at\": \"1640995200000\"}",
  "responseFormat": "JSON"
}
```

## ⚙️ **Stub Configuration**

Configure your Kafka stub like this:

```json
{
  "name": "order-processor",
  "requestTopic": "orders",
  "responseTopic": "orders-response",  // optional - defaults to "{requestTopic}-response"
  "responseType": "callback",
  "callbackUrl": "http://localhost:8080/webhook/test/process-order",
  "callbackHeaders": {
    "Authorization": "Bearer token123",
    "Custom-Header": "value"
  },
  "latency": 1000,  // optional - delay before calling webhook (ms)
  "keyPattern": "order-.*",  // optional - filter by key pattern
  "valuePattern": ".*orderId.*"  // optional - filter by content pattern
}
```

## 🏗️ **Implementation Details**

### **KafkaCallbackService**
- `executeCallback()`: Main method that calls webhook and publishes response
- `executeCallbackAsync()`: Async version for non-blocking operation
- `callWebhook()`: HTTP call to webhook with proper headers and payload
- `parseWebhookResponse()`: Parses webhook JSON response
- `publishWebhookResponseToKafka()`: Publishes webhook response to Kafka topic

### **Auto-Generation Logic**
- **Response Topic**: If not specified, uses `{requestTopic}-response`
- **Response Key**: If webhook doesn't provide key, generates UUID
- **Loop Prevention**: Ensures response topic ≠ request topic

### **Error Handling**
- Webhook failures are logged but don't crash the service
- Invalid webhook responses are gracefully handled
- Retry mechanism available with exponential backoff

## 📊 **Kafka Message Headers**

Published messages include these headers:

```java
{
  "content-format": "JSON",           // from webhook response
  "source-stub-id": "stub-123",       // source stub
  "source-stub-name": "order-processor",
  "response-type": "callback",
  "response-source": "webhook"
}
```

## 🎯 **Usage Scenarios**

### **1. Order Processing**
- Webhook processes order, validates inventory, calculates totals
- Returns processed order with status and updated amount

### **2. Payment Processing**
- Webhook validates payment, charges card, updates balances
- Returns payment confirmation with transaction ID

### **3. Data Transformation**
- Webhook transforms data format (XML to JSON, enrichment, etc.)
- Returns transformed data in desired format

### **4. External System Integration**
- Webhook calls external APIs, databases, or services
- Returns aggregated response from multiple systems

## 🔧 **Testing Your Webhook**

1. **Start the test controller** (included in backend)
2. **Create a stub** with `responseType: "callback"`
3. **Set callbackUrl** to one of the test endpoints
4. **Send a message** to the request topic
5. **Verify response** appears in the response topic

## 🚨 **Best Practices**

- Always validate webhook responses before processing
- Include proper error handling in your webhooks
- Use meaningful keys for response messages
- Set appropriate timeouts for webhook calls
- Log webhook interactions for debugging
- Return consistent response format from webhooks 