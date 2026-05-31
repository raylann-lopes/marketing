package com.north.producoes.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Protege as rotas /api/internal/** com uma API Key estática.
 * Essas rotas são consumidas pelo n8n e não usam JWT.
 * Também permite acesso para usuários ADMIN autenticados via JWT.
 */
@Component
public class InternalApiKeyFilter extends OncePerRequestFilter {

    private static final String INTERNAL_PATH_PREFIX = "/api/internal/";
    private static final String API_KEY_HEADER = "X-Internal-Api-Key";

    @Value("${n8n.api.key:}")
    private String expectedApiKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws IOException, ServletException {
        if (request.getRequestURI().startsWith(INTERNAL_PATH_PREFIX)) {
            
            // 1. Verifica se já existe um usuário ADMIN autenticado (via JWT Filter que rodou antes)
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            boolean isAdmin = auth != null && auth.isAuthenticated() && auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ADMIN"));

            if (isAdmin) {
                chain.doFilter(request, response);
                return;
            }

            // 2. Caso contrário, exige a API Key estática (Cenário do n8n)
            if (!StringUtils.hasText(expectedApiKey)) {
                response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"API key interna não configurada\"}");
                return;
            }

            String providedKey = request.getHeader(API_KEY_HEADER);
            if (providedKey == null || !MessageDigest.isEqual(
                    providedKey.getBytes(StandardCharsets.UTF_8),
                    expectedApiKey.getBytes(StandardCharsets.UTF_8))) {
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"API key inválida ou ausente\"}");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
