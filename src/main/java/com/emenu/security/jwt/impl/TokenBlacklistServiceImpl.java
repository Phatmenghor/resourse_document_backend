package com.emenu.security.jwt.impl;

import com.emenu.security.jwt.JWTGenerator;
import com.emenu.security.jwt.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Date;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenBlacklistServiceImpl implements TokenBlacklistService {

    private final StringRedisTemplate redisTemplate;
    private final JWTGenerator jwtGenerator;

    private static final String TOKEN_PREFIX = "blacklist:token:";
    private static final String USER_PREFIX  = "blacklist:user:";

    /**
     * Blacklist a specific token — stored with TTL equal to remaining token lifetime.
     */
    @Override
    public void blacklistToken(String token, String userIdentifier, String reason) {
        try {
            Date expiry = jwtGenerator.getExpirationDateFromJWT(token);
            long ttlSeconds = Math.max(1, (expiry.getTime() - System.currentTimeMillis()) / 1000);

            redisTemplate.opsForValue().set(
                    TOKEN_PREFIX + token,
                    userIdentifier,
                    Duration.ofSeconds(ttlSeconds));

            log.info("Token blacklisted for user: {} | TTL: {}s | reason: {}", userIdentifier, ttlSeconds, reason);
        } catch (Exception e) {
            log.error("Failed to blacklist token for {}: {}", userIdentifier, e.getMessage());
        }
    }

    /**
     * Invalidate ALL tokens for a user by recording an invalidation timestamp.
     * Any token whose iat < this timestamp is treated as blacklisted.
     * TTL = 1 day (covers any reasonable jwt.expiration window).
     */
    @Override
    public void blacklistAllUserTokens(String userIdentifier, String reason) {
        try {
            redisTemplate.opsForValue().set(
                    USER_PREFIX + userIdentifier,
                    String.valueOf(System.currentTimeMillis()),
                    Duration.ofDays(1));

            log.info("All tokens invalidated for user: {} | reason: {}", userIdentifier, reason);
        } catch (Exception e) {
            log.error("Failed to invalidate all tokens for {}: {}", userIdentifier, e.getMessage());
        }
    }

    /**
     * Returns true if:
     *  1. This specific token key exists in Redis, OR
     *  2. The token was issued before the user-level invalidation timestamp.
     */
    @Override
    public boolean isTokenBlacklisted(String token) {
        if (Boolean.TRUE.equals(redisTemplate.hasKey(TOKEN_PREFIX + token))) {
            return true;
        }
        try {
            String username = jwtGenerator.getUsernameFromJWT(token);
            String invalidatedAtStr = redisTemplate.opsForValue().get(USER_PREFIX + username);
            if (invalidatedAtStr != null) {
                long invalidatedAt = Long.parseLong(invalidatedAtStr);
                Date issuedAt = jwtGenerator.getIssuedAtFromJWT(token);
                return issuedAt != null && issuedAt.getTime() < invalidatedAt;
            }
        } catch (Exception ignored) {}
        return false;
    }

    /**
     * Redis TTL handles expiry automatically — nothing to clean up manually.
     */
    @Override
    public int cleanupExpiredTokens() {
        log.debug("Redis auto-expires blacklisted tokens via TTL — no manual cleanup needed");
        return 0;
    }

    @Override
    public BlacklistStats getBlacklistStats() {
        return new BlacklistStats(0, 0, 0);
    }
}
