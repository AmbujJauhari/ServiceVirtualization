package com.service.virtualization.proxy;

import com.github.tomakehurst.wiremock.WireMockServer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;

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

    private final WireMockServer wireMockServer;

    public ProxyHandler(WireMockServer wireMockServer) {
        this.wireMockServer = wireMockServer;
    }

    /**
     * Handle a proxy request
     *
     * @param request   The incoming request
     * @param response  The outgoing response
     * @param targetUrl The target URL to proxy to
     * @param isProxy   Whether this is a proxy request (true) or API request (false)
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

        // Forward the request to WireMock
        forwardRequestToWireMock(request, response, fullUrl);
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