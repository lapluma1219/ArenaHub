package com.arenahub.controller;

import com.arenahub.dto.ApiResponse;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ApiDocsController {

    @GetMapping("/api/docs")
    public ApiResponse<Map<String, Object>> docs() {
        return ApiResponse.ok(Map.of(
                "auth", List.of(
                        "POST /api/auth/register",
                        "POST /api/auth/login"),
                "player", List.of(
                        "GET /api/players/me",
                        "GET /api/leaderboard?limit=10"),
                "matchmaking", List.of(
                        "POST /api/matchmaking/join",
                        "GET /api/matchmaking/status",
                        "POST /api/matchmaking/leave"),
                "room", List.of(
                        "GET /api/rooms/{roomId}",
                        "POST /api/rooms/{roomId}/finish"),
                "record", List.of("GET /api/matches/me"),
                "websocket", "ws://localhost:8080/ws/rooms/{roomId}?token={jwt}"));
    }
}
