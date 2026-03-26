package com.north.producoes.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@AllArgsConstructor
public class JwtFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // Lê o header "Authorization" da requisição
        String authHeader = request.getHeader("Authorization");

        // Se não tem o header ou não começa com "Bearer ", deixa passar sem autenticar
        // (rotas públicas como /api/auth/login vão cair aqui)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Remove o prefixo "Bearer " e fica só com o token
        String token = authHeader.substring(7);

        // Extrai o email que está dentro do token
        String username = jwtService.extractUsername(token);

        // Só autentica se tem email no token E o usuário ainda não está autenticado nessa requisição
        if (username != null &&
        SecurityContextHolder.getContext().getAuthentication() == null) {

            // Busca o usuário completo no banco pelo email
            UserDetails userDetails =
                    userDetailsService.loadUserByUsername(username);

            // Valida se o token é legítimo e não expirou
            if (jwtService.isTokenValid(token, userDetails)) {

                // Cria o objeto de autenticação com o usuário e suas permissões
                // null no meio = sem credenciais (senha), pois o token já prova a identidade
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());

                // Adiciona detalhes da requisição (IP, session) ao objeto de autenticação
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Registra o usuário como autenticado no contexto do Spring Security
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        // Passa a requisição para o próximo filtro ou controller
        filterChain.doFilter(request, response);
    }
}
