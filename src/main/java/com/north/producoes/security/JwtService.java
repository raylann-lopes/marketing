package com.north.producoes.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtService {
    @Value("${jwt.secret}")
    private String secret;
    @Value("${jwt.expiration}")
    private Long expiration;

    // Converte o secret (string Base64 do application.properties) em uma chave criptográfica
    // usada para assinar e verificar todos os tokens
    private SecretKey getSigningKey(){
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // Gera um novo token JWT para o usuário autenticado
    // O token carrega: email (subject), data de criação e data de expiração
    public String generateToken(UserDetails userDetails){
        return Jwts.builder()
                .subject(userDetails.getUsername())   // email do usuário
                .issuedAt(new Date())                 // momento de geração
                .expiration(new Date(System.currentTimeMillis() + expiration)) // ex: +24h
                .signWith(getSigningKey())             // assina com HMAC-SHA
                .compact();                           // serializa para String
    }

    // Lê o token e extrai o email que foi guardado no subject
    public String extractUsername(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())  // verifica a assinatura
                .build()
                .parseSignedClaims(token)    // decodifica o token
                .getPayload()
                .getSubject();               // retorna o email
    }

    // Verifica se a data de expiração do token já passou
    private boolean isTokenExpired(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getExpiration()
                .before(new Date());  // true se expirou
    }

    // Valida o token: o email bate com o usuário E o token ainda não expirou
    public boolean isTokenValid (String token, UserDetails userDetails){
        String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    }
