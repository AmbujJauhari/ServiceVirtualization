package com.service.virtualization.rest.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.stream.Collectors;

/**
 * Handler for proxy requests that can record interactions using WireMock's recording feature
 */
@Component
public class ProxyHandler {

    private static final Logger logger = LoggerFactory.getLogger(ProxyHandler.class);

    @Value("${rest.api-path}")
    private String apiPath;

    @Value("${rest.proxy-path}")
    private String proxyPath;

    @Value("${wiremock.server.host:localhost}")
    private String wiremockHost;

    @Value("${wiremock.server.port:8080}")
    private int wiremockPort;

    @Value("${auto.record:false}")
    private boolean autoRecord;

    private final RestTemplate restTemplate;

    public ProxyHandler() {
        this.restTemplate = new RestTemplate();

        // Configure RestTemplate to handle gzipped responses
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setBufferRequestBody(false);
        restTemplate.setRequestFactory(requestFactory);

        // Add byte array message converter
        restTemplate.getMessageConverters().add(0, new ByteArrayHttpMessageConverter());
    }

    /**
     * Handle a proxy request
     *
     * @param request   The incoming request
     * @param targetUrl The target URL to proxy to
     * @param isProxy   Whether this is a proxy request (true) or API request (false)
     * @throws IOException If an error occurs
     */
    public ResponseEntity<?> handleRequest(HttpServletRequest request,
                                           String targetUrl, boolean isProxy) throws IOException {

        String method = request.getMethod();
        String path = getRelativePath(request.getRequestURI(), isProxy);

        // Clean up targetUrl to remove any pattern matching symbols
        if (targetUrl != null) {
            // Remove any remaining pattern symbols
            if (targetUrl.startsWith("**/")) {
                targetUrl = targetUrl.substring(3);
            } else if (targetUrl.startsWith("**")) {
                targetUrl = targetUrl.substring(2);
            }
        }

        // Combine filePath and targetUrl for matching
        String fullUrl = targetUrl;
        if (!targetUrl.startsWith("/")) {
            fullUrl = "/" + targetUrl;
        }

        logger.info("Handling {} request for: {}", method, fullUrl);
        logger.debug("Original filePath: {}, Sanitized target URL: {}", path, targetUrl);

        // Forward the request to WireMock
        return forwardRequestToWireMock(request, fullUrl);
    }

    /**
     * Forward a request to WireMock using direct HTTP calls
     */
    private ResponseEntity<?> forwardRequestToWireMock(HttpServletRequest request,
                                                       String url) throws IOException {
        try {
            // Create WireMock URL 
            String wireMockUrl = String.format("http://%s:%d%s", wiremockHost, wiremockPort, url);

            logger.debug("Forwarding request to WireMock at: {}", wireMockUrl);

            // Create headers from request
            HttpHeaders headers = new HttpHeaders();
            Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                headers.addAll(headerName, Collections.list(request.getHeaders(headerName)));
            }

            // Read request body if present
            String body = "";
            if (request.getContentLength() > 0) {
                body = request.getReader().lines().collect(Collectors.joining());
            }

            // Create HTTP entity with headers and body
            HttpEntity<String> entity = new HttpEntity<>(body, headers);

            // Make the request to WireMock and get response as byte array
            ResponseEntity<byte[]> wireMockResponse = restTemplate.exchange(
                    wireMockUrl,
                    HttpMethod.valueOf(request.getMethod()),
                    entity,
                    byte[].class
            );

            String responseBody = new String(wireMockResponse.getBody(), StandardCharsets.UTF_8);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(responseBody);
            if (root.has("callback")) {
                JsonNode callback = root.get("callback");
                String callbackUrl = callback.get("url").asText();
                String callbackMethod = callback.has("method") ? callback.get("method").asText() : "POST";

                ResponseEntity<byte[]> callbackResponse = restTemplate.exchange(callbackUrl, HttpMethod.valueOf(callbackMethod), entity, byte[].class);

                HttpHeaders responseHeaders = new HttpHeaders();
                responseHeaders.putAll(callbackResponse.getHeaders());
                responseHeaders.add("Access-Control-Allow-Origin", "*");

                return new ResponseEntity<>(callbackResponse.getBody(), responseHeaders, callbackResponse.getStatusCode());
            } else {
                HttpHeaders responseHeaders = new HttpHeaders();
                responseHeaders.putAll(wireMockResponse.getHeaders());
                responseHeaders.add("Access-Control-Allow-Origin", "*");

                return new ResponseEntity<>(wireMockResponse.getBody(), responseHeaders, wireMockResponse.getStatusCode());
            }
        } catch (Exception e) {
            logger.error("Error forwarding request to WireMock: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Get the relative filePath by stripping the API or proxy prefix
     */
    private String getRelativePath(String requestUri, boolean isProxy) {
        String prefix = isProxy ? proxyPath : apiPath;
        if (requestUri.startsWith(prefix)) {
            return requestUri.substring(prefix.length());
        }
        return requestUri;
    }
} 