package com.arenahub.controller;

import com.arenahub.dto.ApiResponse;
import com.arenahub.dto.PlayerResponse;
import com.arenahub.security.AuthContext;
import com.arenahub.service.PlayerService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/players")
public class PlayerController {
    private final PlayerService playerService;

    public PlayerController(PlayerService playerService) {
        this.playerService = playerService;
    }

    @GetMapping("/me")
    public ApiResponse<PlayerResponse> me() {
        return ApiResponse.ok(playerService.getMe(AuthContext.get().playerId()));
    }
}
