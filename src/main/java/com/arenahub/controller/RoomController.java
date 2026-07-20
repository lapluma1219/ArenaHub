package com.arenahub.controller;

import com.arenahub.dto.ApiResponse;
import com.arenahub.dto.RoomDtos.FinishRoomRequest;
import com.arenahub.dto.RoomDtos.MatchRecordResponse;
import com.arenahub.dto.RoomDtos.RoomResponse;
import com.arenahub.security.AuthContext;
import com.arenahub.service.RoomService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {
    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @GetMapping("/{roomId}")
    public ApiResponse<RoomResponse> getRoom(@PathVariable Long roomId) {
        return ApiResponse.ok(roomService.getRoom(roomId, AuthContext.get().playerId()));
    }

    @PostMapping("/{roomId}/finish")
    public ApiResponse<MatchRecordResponse> finish(@PathVariable Long roomId, @Valid @RequestBody FinishRoomRequest request) {
        return ApiResponse.ok(roomService.finishRoom(roomId, request.winnerPlayerId(), AuthContext.get().playerId()));
    }
}
