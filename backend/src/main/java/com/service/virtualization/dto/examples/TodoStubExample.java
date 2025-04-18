package com.service.virtualization.dto.examples;

import com.service.virtualization.dto.RestStubDTO;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Example to create a sample stub for todos/3 endpoint with callback functionality
 */
public class TodoStubExample {

    /**
     * Creates a sample RestStubDTO that uses a callback to forward requests to jsonplaceholder endpoint
     * @return A fully configured RestStubDTO ready to be used in Swagger UI
     */
    public static RestStubDTO createTodoStub() {
        // Basic stub properties
        String id = UUID.randomUUID().toString();
        String name = "Todo Item #3 Callback Stub";
        String description = "Forwards requests to jsonplaceholder.typicode.com/todos/3";
        
        // Match conditions - will match GET requests to /todos/3
        Map<String, Object> matchConditions = new HashMap<>();
        matchConditions.put("method", "GET");
        matchConditions.put("url", "/todos/3");
        matchConditions.put("urlMatchType", "exact");
        matchConditions.put("priority", 1);
        
        // Response with callback configuration
        Map<String, Object> response = new HashMap<>();
        
        // Define the callback details
        Map<String, Object> callback = new HashMap<>();
        callback.put("url", "https://jsonplaceholder.typicode.com/todos/3");
        callback.put("method", "GET");
        
        // Add callback to response
        response.put("callback", callback);
        
        // Create the RestStubDTO with all the configured values
        return new RestStubDTO(
                id,                  // id
                name,                // name
                description,         // description
                "admin",             // userId
                false,               // behindProxy
                "HTTP",              // protocol
                Arrays.asList("example", "todo", "callback"), // tags
                "ACTIVE",            // status
                LocalDateTime.now(), // createdAt
                LocalDateTime.now(), // updatedAt
                null,                // wiremockMappingId
                matchConditions,     // matchConditions
                response             // response
        );
    }
    
    /**
     * @return A sample JSON representation of the stub for use in Swagger UI
     */
    public static String getSwaggerJson() {
        return "{\n" +
                "  \"name\": \"Todo Item #3 Callback Stub\",\n" +
                "  \"description\": \"Forwards requests to jsonplaceholder.typicode.com/todos/3\",\n" +
                "  \"userId\": \"admin\",\n" +
                "  \"behindProxy\": false,\n" +
                "  \"protocol\": \"HTTP\",\n" +
                "  \"tags\": [\"example\", \"todo\", \"callback\"],\n" +
                "  \"status\": \"ACTIVE\",\n" +
                "  \"matchConditions\": {\n" +
                "    \"method\": \"GET\",\n" +
                "    \"url\": \"/todos/3\",\n" +
                "    \"urlMatchType\": \"exact\",\n" +
                "    \"priority\": 1\n" +
                "  },\n" +
                "  \"response\": {\n" +
                "    \"callback\": {\n" +
                "      \"url\": \"https://jsonplaceholder.typicode.com/todos/3\",\n" +
                "      \"method\": \"GET\"\n" +
                "    }\n" +
                "  }\n" +
                "}";
    }
} 