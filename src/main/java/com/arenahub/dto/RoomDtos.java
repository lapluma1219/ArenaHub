package com.arenahub.dto;

import com.arenahub.entity.GameRoom;
import com.arenahub.entity.MatchRecord;
import com.arenahub.entity.RoomStatus;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public final class RoomDtos {
    private RoomDtos() {
    }

    public record MatchmakingResponse(String state, Long roomId, PlayerResponse opponent) {
    }

    public record RoomResponse(
            Long id,
            RoomStatus status,
            PlayerResponse playerOne,
            PlayerResponse playerTwo,
            LocalDateTime createdAt,
            LocalDateTime finishedAt) {
        public static RoomResponse from(GameRoom room) {
            return new RoomResponse(
                    room.getId(),
                    room.getStatus(),
                    PlayerResponse.from(room.getPlayerOne()),
                    PlayerResponse.from(room.getPlayerTwo()),
                    room.getCreatedAt(),
                    room.getFinishedAt());
        }
    }

    public record FinishRoomRequest(@NotNull Long winnerPlayerId) {
    }

    public record MatchRecordResponse(
            Long id,
            Long roomId,
            PlayerResponse winner,
            PlayerResponse loser,
            int winnerRatingAfter,
            int loserRatingAfter,
            LocalDateTime finishedAt) {
        public static MatchRecordResponse from(MatchRecord record) {
            return new MatchRecordResponse(
                    record.getId(),
                    record.getRoom().getId(),
                    PlayerResponse.from(record.getWinner()),
                    PlayerResponse.from(record.getLoser()),
                    record.getWinnerRatingAfter(),
                    record.getLoserRatingAfter(),
                    record.getFinishedAt());
        }
    }
}
