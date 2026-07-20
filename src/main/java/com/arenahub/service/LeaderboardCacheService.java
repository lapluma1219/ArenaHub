package com.arenahub.service;

import com.arenahub.dto.PlayerResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class LeaderboardCacheService {
    private static final String KEY_PREFIX = "arenahub:leaderboard:";
    private final Optional<StringRedisTemplate> redisTemplate;
    private final Duration ttl;

    public LeaderboardCacheService(
            Optional<StringRedisTemplate> redisTemplate,
            @Value("${arenahub.cache.leaderboard-ttl-seconds}") long ttlSeconds) {
        this.redisTemplate = redisTemplate;
        this.ttl = Duration.ofSeconds(ttlSeconds);
    }

    public Optional<List<PlayerResponse>> get(int limit) {
        try {
            return redisTemplate
                    .map(redis -> redis.opsForValue().get(KEY_PREFIX + limit))
                    .filter(value -> !value.isBlank())
                    .map(this::decode);
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    public void put(int limit, List<PlayerResponse> players) {
        try {
            redisTemplate.ifPresent(redis -> redis.opsForValue().set(KEY_PREFIX + limit, encode(players), ttl));
        } catch (RuntimeException ignored) {
            // Redis 是演示缓存层，异常时回退到数据库查询。
        }
    }

    public void evictAll() {
        try {
            redisTemplate.ifPresent(redis -> {
                var keys = redis.keys(KEY_PREFIX + "*");
                if (keys != null && !keys.isEmpty()) {
                    redis.delete(keys);
                }
            });
        } catch (RuntimeException ignored) {
            // 缓存失效失败不影响主业务提交。
        }
    }

    private String encode(List<PlayerResponse> players) {
        return players.stream()
                .map(player -> player.id()
                        + "," + b64(player.username())
                        + "," + b64(player.nickname())
                        + "," + player.rating()
                        + "," + player.wins()
                        + "," + player.losses())
                .reduce((left, right) -> left + ";" + right)
                .orElse("");
    }

    private List<PlayerResponse> decode(String value) {
        return List.of(value.split(";"))
                .stream()
                .filter(item -> !item.isBlank())
                .map(item -> {
                    String[] parts = item.split(",");
                    return new PlayerResponse(
                            Long.valueOf(parts[0]),
                            fromB64(parts[1]),
                            fromB64(parts[2]),
                            Integer.parseInt(parts[3]),
                            Integer.parseInt(parts[4]),
                            Integer.parseInt(parts[5]));
                })
                .toList();
    }

    private String b64(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private String fromB64(String value) {
        return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
    }
}
