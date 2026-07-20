package com.arenahub.controller;

import com.arenahub.dto.ApiResponse;
import com.arenahub.dto.RoomDtos.MatchRecordResponse;
import com.arenahub.security.AuthContext;
import com.arenahub.service.RoomService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/matches")
public class MatchRecordController {
    private final RoomService roomService;

    public MatchRecordController(RoomService roomService) {
        this.roomService = roomService;
    }

    @GetMapping("/me")
    public ApiResponse<List<MatchRecordResponse>> myMatches() {
        return ApiResponse.ok(roomService.myMatches(AuthContext.get().playerId()));
    }
}
