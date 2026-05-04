package com.north.producoes.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Protege as rotas /api/internal/** com uma API Key estática.
 * Essas rotas são consumidas pelo n8n e não usam JWT.
 */
@Component
public class InternalApiKeyFilter extends OncePerRequestFilter {

    private static final String INTERNAL_PATH_PREFIX = "/api/internal/";
    private static final String API_KEY_HEADER = "X-Internal-Api-Key";

    @Value("${n8n.api.key:}")
    private String expectedApiKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws IOException, ServletException {
        if (request.getRequestURI().startsWith(INTERNAL_PATH_PREFIX)) {
            if (!StringUtils.hasText(expectedApiKey)) {
                response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"API key interna não configurada\"}");
                return;
            }

            String providedKey = request.getHeader(API_KEY_HEADER);
            if (providedKey == null || !providedKey.equals(expectedApiKey)) {
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"API key inválida ou ausente\"}");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
