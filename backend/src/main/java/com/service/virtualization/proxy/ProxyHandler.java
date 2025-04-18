package com.service.virtualization.proxy;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.recording.RecordingStatusResult;
import com.github.tomakehurst.wiremock.recording.SnapshotRecordResult;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.service.virtualization.model.Protocol;
import com.service.virtualization.model.RecordingConfig;
import com.service.virtualization.model.Recording;
import com.service.virtualization.model.RequestHeader;
import com.service.virtualization.model.ResponseHeader;
import com.service.virtualization.rest.service.RecordingConfigService;
import com.service.virtualization.rest.service.RecordingService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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
    
    @Value("${auto.record:false}")
    private boolean autoRecord;
    
    private final RecordingConfigService recordingConfigService;
    private final RecordingService recordingService;
    private final WireMockServer wireMockServer;
    
    public ProxyHandler(RecordingConfigService recordingConfigService, 
                        RecordingService recordingService,
                        WireMockServer wireMockServer) {
        this.recordingConfigService = recordingConfigService;
        this.recordingService = recordingService;
        this.wireMockServer = wireMockServer;
    }
    
    /**
     * Handle a proxy request
     * 
     * @param request The incoming request
     * @param response The outgoing response
     * @param targetUrl The target URL to proxy to
     * @param isProxy Whether this is a proxy request (true) or API request (false)
     * @throws IOException If an error occurs
     */
    public void handleRequest(HttpServletRequest request, HttpServletResponse response, 
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
        
        // Combine path and targetUrl for matching
        String fullUrl = targetUrl;
        if (!targetUrl.startsWith("/")) {
            fullUrl = "/" + targetUrl;
        }
        
        logger.info("Handling {} request for: {}", method, fullUrl);
        logger.debug("Original path: {}, Sanitized target URL: {}", path, targetUrl);
        
        // Check if there's a matching stub
        boolean stubFound = false;
        
        for (StubMapping stub : wireMockServer.getStubMappings()) {
            String stubUrl = stub.getRequest().getUrl();
            String stubMethod = stub.getRequest().getMethod().getName();
            
            logger.debug("Comparing against stub: {} {}", stubMethod, stubUrl);
            
            if (stubMethod.equals(method) && 
                (stubUrl.equals(fullUrl) || 
                 stubUrl.equals(fullUrl.substring(1)) || 
                 stubUrl.endsWith(fullUrl))) {
                
                logger.info("Found matching stub: {} {} with ID {}", 
                    stubMethod, stubUrl, stub.getId());
                stubFound = true;
                break;
            }
        }
        
        if (stubFound) {
            logger.info("Using matching stub for {} {}", method, fullUrl);
        }
        
        // If no matching stub, proceed with recording logic
        boolean shouldRecord = recordingConfigService.shouldRecordPath(path) || autoRecord;
        
        // Start recording if needed
        String recordingId = null;
        if (shouldRecord && !stubFound) {
            recordingId = startRecording(targetUrl);
            logger.info("Started WireMock recording for {} {} (ID: {})", method, fullUrl, recordingId);
        }
        
        try {
            // Forward the request to WireMock
            forwardRequestToWireMock(request, response, fullUrl);
        } finally {
            // Stop recording if we started one and save recordings to database
            if (shouldRecord && recordingId != null) {
                stopRecordingAndSaveToDatabase(recordingId, targetUrl, method, path, isProxy, request.getRemoteAddr());
                logger.info("Stopped WireMock recording and saved to database (ID: {})", recordingId);
            }
        }
    }
    
    /**
     * Forward a request to WireMock using URL forwarding
     */
    private void forwardRequestToWireMock(HttpServletRequest request, HttpServletResponse response, 
                                        String url) throws IOException {
        try {
            // Create WireMock URL 
            String wireMockUrl = String.format("http://localhost:%d%s", wireMockServer.port(), url);
            
            logger.debug("Forwarding request to WireMock at: {}", wireMockUrl);
            
            // Set CORS headers
            response.setHeader("Access-Control-Allow-Origin", request.getHeader("Origin"));
            response.setHeader("Access-Control-Allow-Credentials", "true");
            response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD, PATCH");
            response.setHeader("Access-Control-Allow-Headers", "*");
            
            // Handle OPTIONS requests separately
            if (request.getMethod().equals("OPTIONS")) {
                response.setStatus(HttpServletResponse.SC_OK);
                return;
            }
            
            // Redirect to WireMock
            response.sendRedirect(wireMockUrl);
            
            logger.debug("Request redirected to WireMock");
        } catch (Exception e) {
            logger.error("Error forwarding request to WireMock: {}", e.getMessage(), e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("Error proxying request: " + e.getMessage());
        }
    }
    
    /**
     * Start WireMock recording
     * 
     * @param targetUrl The target URL to record from
     * @return The recording ID
     */
    private String startRecording(String targetUrl) {
        try {
            // Generate unique recording ID
            String recordingId = UUID.randomUUID().toString();
            
            // Create URL for target
            URI targetUri = new URI(targetUrl);
            URL targetUrlObj = targetUri.toURL();
            
            // Check if this path should use HTTPS from a recording config
            String path = targetUrlObj.getPath();
            
            // Find a matching recording config with HTTPS settings
            Optional<RecordingConfig> configOpt = recordingConfigService.findAllConfigs().stream()
                .filter(c -> c.active() && c.matches(path) && c.useHttps() && 
                       c.certificateData() != null && c.certificateData().length > 0 &&
                       c.certificatePassword() != null)
                .findFirst();
            
            // Start recording using WireMock's API
            wireMockServer.startRecording(targetUrlObj.toString());
            
            // Set recording options
            wireMockServer.setGlobalFixedDelay(0); // No artificial delay
            
            // Apply HTTPS settings if available
            if (configOpt.isPresent()) {
                RecordingConfig config = configOpt.get();
                logger.info("Using HTTPS settings from config: {}", config.name());
                
                // Note: The certificate should already be configured in WireMockConfig
                // Here we're just logging that we're using HTTPS for this recording
            }
            
            return recordingId;
        } catch (Exception e) {
            logger.error("Failed to start WireMock recording", e);
            return null;
        }
    }
    
    /**
     * Stop WireMock recording and save captured interactions to database
     */
    private void stopRecordingAndSaveToDatabase(String recordingId, String targetUrl, 
                                              String method, String path, 
                                              boolean behindProxy, String sourceIp) {
        try {
            // Get the current recorded events before stopping
            RecordingStatusResult recordingStatus = wireMockServer.getRecordingStatus();
            List<ServeEvent> recordedEvents = null;
            
            if (recordingStatus != null && recordingStatus.getStatus().equals("Recording")) {
                recordedEvents = wireMockServer.getAllServeEvents();
            }
            
            // Stop recording
            SnapshotRecordResult result = wireMockServer.stopRecording();
            
            // Extract stub mappings from recording result
            List<StubMapping> stubMappings = result.getStubMappings();
            if (stubMappings != null && !stubMappings.isEmpty()) {
                logger.info("Captured {} stub mappings from WireMock recording", stubMappings.size());
                
                // Convert WireMock stub mappings to our Recording model
                for (StubMapping stubMapping : stubMappings) {
                    Recording recording = convertStubMappingToRecording(
                        stubMapping, 
                        recordingId, 
                        targetUrl, 
                        method, 
                        path, 
                        behindProxy, 
                        sourceIp,
                        recordedEvents
                    );
                    
                    // Save to database
                    Recording savedRecording = recordingService.createRecording(recording);
                    logger.debug("Saved recording to database: {}", savedRecording.id());
                }
            } else {
                logger.warn("No stub mappings found in WireMock recording");
            }
        } catch (Exception e) {
            logger.error("Failed to stop WireMock recording or save to database", e);
        }
    }
    
    /**
     * Convert a WireMock StubMapping to our Recording model
     */
    private Recording convertStubMappingToRecording(StubMapping stubMapping, 
                                                  String recordingId, 
                                                  String targetUrl,
                                                  String method, 
                                                  String path, 
                                                  boolean behindProxy, 
                                                  String sourceIp,
                                                  List<ServeEvent> recordedEvents) {
        // Extract request data
        Map<String, Object> requestData = new HashMap<>();
        if (stubMapping.getRequest() != null) {
            requestData.put("method", stubMapping.getRequest().getMethod().getName());
            requestData.put("path", stubMapping.getRequest().getUrl());
            
            // Extract headers if available
            if (stubMapping.getRequest().getHeaders() != null) {
                List<RequestHeader> headers = new ArrayList<>();
                stubMapping.getRequest().getHeaders().forEach((name, matcher) -> {
                    headers.add(new RequestHeader(name, matcher.getExpected()));
                });
                requestData.put("headers", headers);
            }
            
            // Extract body if available
            if (stubMapping.getRequest().getBodyPatterns() != null && !stubMapping.getRequest().getBodyPatterns().isEmpty()) {
                requestData.put("body", stubMapping.getRequest().getBodyPatterns().get(0).getExpected());
            }
        } else {
            // Fallback to method and path from request
            requestData.put("method", method);
            requestData.put("path", path);
        }
        
        // Extract response data
        Map<String, Object> responseData = new HashMap<>();
        if (stubMapping.getResponse() != null) {
            responseData.put("status", stubMapping.getResponse().getStatus());
            
            // Extract headers if available
            if (stubMapping.getResponse().getHeaders() != null) {
                List<ResponseHeader> headers = new ArrayList<>();
                stubMapping.getResponse().getHeaders().all().forEach(header -> {
                    headers.add(new ResponseHeader(header.key(), header.firstValue()));
                });
                responseData.put("headers", headers);
            }
            
            // Extract body if available
            if (stubMapping.getResponse().getBody() != null) {
                responseData.put("body", stubMapping.getResponse().getBody());
            }
        }
        
        // Try to get more details from the recorded events if available
        if (recordedEvents != null) {
            recordedEvents.stream()
                .filter(event -> event.getStubMapping() != null && 
                                event.getStubMapping().getId().equals(stubMapping.getId()))
                .findFirst()
                .ifPresent(event -> {
                    // Enhance request data
                    if (event.getRequest() != null) {
                        requestData.putIfAbsent("path", event.getRequest().getUrl());
                        requestData.putIfAbsent("method", event.getRequest().getMethod().getName());
                        
                        // Add query parameters if available
                        if (event.getRequest().getQueryParams() != null && !event.getRequest().getQueryParams().isEmpty()) {
                            requestData.put("queryParams", event.getRequest().getQueryParams());
                        }
                    }
                    
                    // Enhance response data
                    if (event.getResponse() != null) {
                        responseData.putIfAbsent("status", event.getResponse().getStatus());
                        
                        // Add body if available
                        if (event.getResponse().getBody() != null) {
                            responseData.putIfAbsent("body", event.getResponse().getBodyAsString());
                        }
                    }
                });
        }
        
        // Generate a name based on method and path
        String name = "Recorded " + requestData.get("method") + " " + requestData.get("path");
        
        // Create the recording object
        return new Recording(
            null,                       // id - let database generate
            name,                       // name
            "Recorded from " + targetUrl, // description
            "default",                  // userId
            behindProxy,                // behindProxy
            Protocol.HTTP,              // protocol
            new HashMap<>(),            // protocolData
            LocalDateTime.now(),        // createdAt
            LocalDateTime.now(),        // updatedAt
            recordingId,                // sessionId - use the recording ID
            LocalDateTime.now(),        // recordedAt
            false,                      // convertedToStub
            null,                       // convertedStubId
            sourceIp,                   // sourceIp
            requestData,                // requestData
            responseData                // responseData
        );
    }
    
    /**
     * Get the relative path by stripping the API or proxy prefix
     */
    private String getRelativePath(String requestUri, boolean isProxy) {
        String prefix = isProxy ? proxyPath : apiPath;
        if (requestUri.startsWith(prefix)) {
            return requestUri.substring(prefix.length());
        }
        return requestUri;
    }
} 