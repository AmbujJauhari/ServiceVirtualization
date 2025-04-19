package com.service.virtualization.rest.controller;

import com.service.virtualization.rest.service.ProxyHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerMapping;

/**
 * Controller to handle proxy and API pass-through requests
 */
@RestController
public class ProxyController {
    
    private static final Logger logger = LoggerFactory.getLogger(ProxyController.class);
    
    @Value("${rest.api-path}")
    private String apiPath;
    
    @Value("${rest.proxy-path}")
    private String proxyPath;
    
    private final ProxyHandler proxyHandler;
    
    public ProxyController(ProxyHandler proxyHandler) {
        this.proxyHandler = proxyHandler;
    }
    
    /**
     * Handle API endpoint requests
     * This provides direct access to external systems
     * 
     * @param request the incoming request
     * @param response the outgoing response
     * @throws IOException if an error occurs
     */
    @RequestMapping(value = "${rest.api-path}/**", method = {RequestMethod.GET, RequestMethod.POST,
            RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH, RequestMethod.HEAD, 
            RequestMethod.OPTIONS})
    public void handleApiRequest(
            HttpServletRequest request, 
            HttpServletResponse response) throws IOException {
        
        // Extract the path variables after the api-path
        String path = extractPathFromPattern(request);
        
        // Handle OPTIONS requests for CORS preflight
        if (request.getMethod().equals("OPTIONS")) {
            response.setHeader("Access-Control-Allow-Origin", request.getHeader("Origin"));
            response.setHeader("Access-Control-Allow-Credentials", "true");
            response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD, PATCH");
            response.setHeader("Access-Control-Allow-Headers", "*");
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }
        
        logger.info("API request received for path: {}", path);
        
        // Process the request (recording will be handled based on path configuration)
        proxyHandler.handleRequest(request, response, path, false);
    }
    
    /**
     * Handle proxy endpoint requests
     * This provides proxy access to external systems
     * 
     * @param request the incoming request
     * @param response the outgoing response
     * @throws IOException if an error occurs
     */
    @RequestMapping(value = "${rest.proxy-path}/**", method = {RequestMethod.GET, RequestMethod.POST, 
            RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH, RequestMethod.HEAD, 
            RequestMethod.OPTIONS})
    public void handleProxyRequest(
            HttpServletRequest request, 
            HttpServletResponse response) throws IOException {
        
        // Extract the path variables after the proxy-path
        String path = extractPathFromPattern(request);
        
        logger.info("Proxy request received for path: {}", path);
        
        // Process the request (recording will be handled based on path configuration)
        proxyHandler.handleRequest(request, response, path, true);
    }
    
    /**
     * Extract the path after the controller's base mapping
     */
    private String extractPathFromPattern(HttpServletRequest request) {
        String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        String bestMatchPattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        
        String extractedPath = new AntPathMatcher().extractPathWithinPattern(bestMatchPattern, path);
        
        // Remove any ** pattern symbols that might remain
        if (extractedPath != null && extractedPath.startsWith("**/")) {
            extractedPath = extractedPath.substring(3);
        } else if (extractedPath != null && extractedPath.startsWith("**")) {
            extractedPath = extractedPath.substring(2);
        }
        
        logger.debug("Original path: {}, Best match: {}, Extracted: {}", path, bestMatchPattern, extractedPath);
        
        return extractedPath;
    }
} 