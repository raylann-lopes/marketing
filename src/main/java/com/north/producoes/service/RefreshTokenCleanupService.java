package com.north.producoes.service;

import com.north.producoes.security.refreshToken.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenCleanupService {

    private final RefreshTokenRepository refreshTokenRepository;

    // Todo domingo às 3h — remove tokens já expirados há mais de 7 dias
    @Scheduled(cron = "0 0 3 * * SUN")
    @Transactional
    public void purgeExpiredTokens() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(7);
        int deleted = refreshTokenRepository.deleteByExpiresAtBefore(threshold);
        log.info("Refresh token cleanup: {} tokens expirados removidos.", deleted);
    }
}
