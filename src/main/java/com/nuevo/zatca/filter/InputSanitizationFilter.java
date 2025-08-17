package com.nuevo.zatca.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public class InputSanitizationFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Wrap request to allow body/param inspection
        SanitizedRequestWrapper sanitizedRequest = new SanitizedRequestWrapper(httpRequest);

        // ✅ 1. Validate Query Params
        sanitizedRequest.getParameterMap().forEach((key, values) -> {
            for (String value : values) {
                if (InputValidator.isMalicious(value)) {
                    try {
                        sendBadRequest(httpResponse, "Malicious input detected in query parameter: " + key);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    return;
                }
            }
        });



        // ✅ 2. Validate Headers
        var headerNames = httpRequest.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String headerValue = httpRequest.getHeader(headerName);
            if (InputValidator.isMalicious(headerValue)) {
                sendBadRequest(httpResponse, "Malicious input detected in header: " + headerName);
                return;
            }
        }

        // ✅ 3. Validate Body
        String body = sanitizedRequest.getRequestBody();
        if (InputValidator.isMalicious(body)) {
            sendBadRequest(httpResponse, "Malicious input detected in request body");
            return;
        }

        // Continue filter chain if safe
        if (httpResponse.isCommitted()) {
            return;
        }

        chain.doFilter(sanitizedRequest, httpResponse);
    }

    private void sendBadRequest(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\": \"" + message + "\"}");
        response.getWriter().flush();
        response.getWriter().close();
    }
}
