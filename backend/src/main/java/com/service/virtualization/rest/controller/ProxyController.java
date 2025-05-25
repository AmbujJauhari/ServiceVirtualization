package com.service.virtualization.rest.controller;

import com.service.virtualization.rest.service.ProxyHandler;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;

import java.io.IOException;

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
     * @throws IOException if an error occurs
     */
    @RequestMapping(value = "${rest.api-path}/**", method = {RequestMethod.GET, RequestMethod.POST,
            RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH, RequestMethod.HEAD,
            RequestMethod.OPTIONS})
    public ResponseEntity<?> handleApiRequest(
            HttpServletRequest request) throws IOException {

        // Extract the filePath variables after the api-path
        String path = extractPathFromPattern(request);

        // Handle OPTIONS requests for CORS preflight

        logger.info("API request received for filePath: {}", path);

        // Process the request (recording will be handled based on filePath configuration)
        return proxyHandler.handleRequest(request, path, false);
    }

    /**
     * Handle proxy endpoint requests
     * This provides proxy access to external systems
     *
     * @param request the incoming request
     * @throws IOException if an error occurs
     */
    @RequestMapping(value = "${rest.proxy-path}/**", method = {RequestMethod.GET, RequestMethod.POST,
            RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH, RequestMethod.HEAD,
            RequestMethod.OPTIONS})
    public ResponseEntity<?> handleProxyRequest(
            HttpServletRequest request) throws IOException {

        // Extract the filePath variables after the proxy-path
        String path = extractPathFromPattern(request);

        logger.info("Proxy request received for filePath: {}", path);

        // Process the request (recording will be handled based on filePath configuration)
        return proxyHandler.handleRequest(request, path, true);
    }

    /**
     * Extract the filePath after the controller's base mapping
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

        logger.debug("Original filePath: {}, Best match: {}, Extracted: {}", path, bestMatchPattern, extractedPath);

        return extractedPath;
    }
} 