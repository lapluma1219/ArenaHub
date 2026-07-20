package com.arenahub.service;

import com.arenahub.dto.PlayerResponse;
import com.arenahub.entity.PlayerProfile;
import com.arenahub.exception.BusinessException;
import com.arenahub.repository.PlayerProfileRepository;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlayerService {
    private final PlayerProfileRepository playerRepository;
    private final LeaderboardCacheService leaderboardCacheService;

    public PlayerService(PlayerProfileRepository playerRepository, LeaderboardCacheService leaderboardCacheService) {
        this.playerRepository = playerRepository;
        this.leaderboardCacheService = leaderboardCacheService;
    }

    @Transactional(readOnly = true)
    public PlayerProfile requirePlayer(Long playerId) {
        return playerRepository.findById(playerId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "玩家不存在"));
    }

    @Transactional(readOnly = true)
    public PlayerResponse getMe(Long playerId) {
        return PlayerResponse.from(requirePlayer(playerId));
    }

    @Transactional(readOnly = true)
    public List<PlayerResponse> leaderboard(int limit) {
        int size = Math.max(1, Math.min(limit, 100));
        return leaderboardCacheService.get(size).orElseGet(() -> {
            List<PlayerResponse> players = playerRepository.findAll(PageRequest.of(0, size, Sort.by(Sort.Direction.DESC, "rating")
                        .and(Sort.by(Sort.Direction.DESC, "wins"))
                        .and(Sort.by(Sort.Direction.ASC, "losses"))))
                .stream()
                .map(PlayerResponse::from)
                .toList();
            leaderboardCacheService.put(size, players);
            return players;
        });
    }
}
