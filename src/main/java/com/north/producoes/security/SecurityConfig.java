package com.north.producoes.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                    JwtFilter jwtFilter,
                                                    InternalApiKeyFilter internalApiKeyFilter) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // Desabilita CSRF — não necessário em APIs stateless (sem sessão/cookie)
                .csrf(csrf -> csrf.disable())

                // Define que a API não mantém sessão no servidor
                // cada requisição precisa se autenticar pelo token
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        // Rotas internas protegidas pelo InternalApiKeyFilter, não por JWT
                        .requestMatchers("/api/internal/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                        .anyRequest().authenticated())

                // Registra o JwtFilter para rodar antes do filtro padrão do Spring Security
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                // InternalApiKeyFilter roda agora depois do JwtFilter
                .addFilterAfter(internalApiKeyFilter, JwtFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        // Rotas internas (n8n/automações): sem restrição de origem
        CorsConfiguration internalConfig = new CorsConfiguration();
        internalConfig.setAllowedOriginPatterns(List.of("*"));
        internalConfig.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        internalConfig.setAllowedHeaders(List.of("*"));
        internalConfig.setAllowCredentials(false);

        // Rotas da aplicação web: apenas o frontend
        CorsConfiguration webConfig = new CorsConfiguration();
        webConfig.setAllowedOrigins(List.of("http://localhost:5173", "http://localhost"));
        webConfig.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        webConfig.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Internal-Api-Key"));
        webConfig.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/internal/**", internalConfig);
        source.registerCorsConfiguration("/**", webConfig);
        return source;
    }

    // BCrypt para hash de senhas — usado no UserService ao salvar usuário
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Expõe o AuthenticationManager como bean — usado no AuthController para autenticar no login
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
