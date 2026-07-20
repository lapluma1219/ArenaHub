package com.arenahub.controller;

import com.arenahub.dto.ApiResponse;
import com.arenahub.dto.PlayerResponse;
import com.arenahub.service.PlayerService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/leaderboard")
public class LeaderboardController {
    private final PlayerService playerService;

    public LeaderboardController(PlayerService playerService) {
        this.playerService = playerService;
    }

    @GetMapping
    public ApiResponse<List<PlayerResponse>> leaderboard(@RequestParam(defaultValue = "10") int limit) {
        return ApiResponse.ok(playerService.leaderboard(limit));
    }
}
