package com.arenahub.controller;

import com.arenahub.dto.ApiResponse;
import com.arenahub.dto.RoomDtos.MatchmakingResponse;
import com.arenahub.security.AuthContext;
import com.arenahub.service.MatchmakingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/matchmaking")
public class MatchmakingController {
    private final MatchmakingService matchmakingService;

    public MatchmakingController(MatchmakingService matchmakingService) {
        this.matchmakingService = matchmakingService;
    }

    @PostMapping("/join")
    public ApiResponse<MatchmakingResponse> join() {
        return ApiResponse.ok(matchmakingService.join(AuthContext.get().playerId()));
    }

    @GetMapping("/status")
    public ApiResponse<MatchmakingResponse> status() {
        return ApiResponse.ok(matchmakingService.status(AuthContext.get().playerId()));
    }

    @PostMapping("/leave")
    public ApiResponse<MatchmakingResponse> leave() {
        return ApiResponse.ok(matchmakingService.leave(AuthContext.get().playerId()));
    }
}
