package com.north.producoes.security.refreshToken;

import com.north.producoes.exception.InvalidRefreshTokenException;
import com.north.producoes.entity.UserEntity;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Service
@AllArgsConstructor
public class RefreshTokenService {
    private static final int REFRESH_TOKEN_TTL_DAYS = 7;
    private static final int REFRESH_TOKEN_BYTES = 32;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public String generate(UserEntity user) {
        String rawToken = generateRawToken();

        RefreshTokenEntity refreshToken = new RefreshTokenEntity();
        refreshToken.setTokenHash(hashToken(rawToken));
        refreshToken.setExpiresAt(LocalDateTime.now().plusDays(REFRESH_TOKEN_TTL_DAYS));
        refreshToken.setUser(user);
        refreshTokenRepository.save(refreshToken);

        return rawToken;
    }

    @Transactional
    public RotatedRefreshToken rotate(String rawToken) {
        String currentTokenHash = hashToken(rawToken);

        RefreshTokenEntity current = refreshTokenRepository.findByTokenHashForUpdate(currentTokenHash)
                .orElseThrow(() -> new InvalidRefreshTokenException("Refresh token invalido"));

        if (current.getRevokedAt() != null) {
            throw new InvalidRefreshTokenException("Refresh token revogado");
        }

        if (current.getExpiresAt().isBefore(LocalDateTime.now())) {
            current.setRevokedAt(LocalDateTime.now());
            refreshTokenRepository.save(current);
            throw new InvalidRefreshTokenException("Refresh token expirado");
        }

        String newRawToken = generateRawToken();
        String newTokenHash = hashToken(newRawToken);

        current.setRevokedAt(LocalDateTime.now());
        current.setReplacedByTokenHash(newTokenHash);
        refreshTokenRepository.save(current);

        RefreshTokenEntity next = new RefreshTokenEntity();
        next.setTokenHash(newTokenHash);
        next.setExpiresAt(LocalDateTime.now().plusDays(REFRESH_TOKEN_TTL_DAYS));
        next.setUser(current.getUser());
        refreshTokenRepository.save(next);

        return new RotatedRefreshToken(current.getUser(), newRawToken);
    }

    private String generateRawToken() {
        byte[] randomBytes = new byte[REFRESH_TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algoritmo SHA-256 indisponivel", e);
        }
    }

    public record RotatedRefreshToken(UserEntity user, String refreshToken) {
    }
}
