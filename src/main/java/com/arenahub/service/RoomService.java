package com.arenahub.service;

import com.arenahub.dto.RoomDtos.MatchRecordResponse;
import com.arenahub.dto.RoomDtos.RoomResponse;
import com.arenahub.entity.GameRoom;
import com.arenahub.entity.MatchRecord;
import com.arenahub.entity.PlayerProfile;
import com.arenahub.entity.RoomStatus;
import com.arenahub.exception.BusinessException;
import com.arenahub.repository.GameRoomRepository;
import com.arenahub.repository.MatchRecordRepository;
import com.arenahub.repository.PlayerProfileRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoomService {
    private static final int WIN_DELTA = 25;
    private static final int LOSE_DELTA = 15;

    private final GameRoomRepository roomRepository;
    private final PlayerProfileRepository playerRepository;
    private final MatchRecordRepository matchRecordRepository;
    private final LeaderboardCacheService leaderboardCacheService;

    public RoomService(
            GameRoomRepository roomRepository,
            PlayerProfileRepository playerRepository,
            MatchRecordRepository matchRecordRepository,
            LeaderboardCacheService leaderboardCacheService) {
        this.roomRepository = roomRepository;
        this.playerRepository = playerRepository;
        this.matchRecordRepository = matchRecordRepository;
        this.leaderboardCacheService = leaderboardCacheService;
    }

    @Transactional(readOnly = true)
    public RoomResponse getRoom(Long roomId, Long requesterPlayerId) {
        GameRoom room = requireRoom(roomId);
        assertParticipant(room, requesterPlayerId);
        return RoomResponse.from(room);
    }

    @Transactional
    public MatchRecordResponse finishRoom(Long roomId, Long winnerPlayerId, Long requesterPlayerId) {
        GameRoom room = roomRepository.findByIdForUpdate(roomId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "房间不存在"));
        assertParticipant(room, requesterPlayerId);
        if (room.getStatus() == RoomStatus.FINISHED) {
            throw new BusinessException(HttpStatus.CONFLICT, "房间已结算");
        }
        if (!isParticipant(room, winnerPlayerId)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "胜者必须是本房间玩家");
        }

        PlayerProfile winner = room.getPlayerOne().getId().equals(winnerPlayerId) ? room.getPlayerOne() : room.getPlayerTwo();
        PlayerProfile loser = room.getPlayerOne().getId().equals(winnerPlayerId) ? room.getPlayerTwo() : room.getPlayerOne();

        winner.setWins(winner.getWins() + 1);
        winner.setRating(winner.getRating() + WIN_DELTA);
        loser.setLosses(loser.getLosses() + 1);
        loser.setRating(Math.max(0, loser.getRating() - LOSE_DELTA));
        playerRepository.save(winner);
        playerRepository.save(loser);

        room.setStatus(RoomStatus.FINISHED);
        room.setFinishedAt(LocalDateTime.now());

        MatchRecord record = new MatchRecord();
        record.setRoom(room);
        record.setWinner(winner);
        record.setLoser(loser);
        record.setWinnerRatingAfter(winner.getRating());
        record.setLoserRatingAfter(loser.getRating());
        leaderboardCacheService.evictAll();
        return MatchRecordResponse.from(matchRecordRepository.save(record));
    }

    @Transactional(readOnly = true)
    public List<MatchRecordResponse> myMatches(Long playerId) {
        return matchRecordRepository.findByWinnerIdOrLoserIdOrderByFinishedAtDesc(playerId, playerId)
                .stream()
                .map(MatchRecordResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public GameRoom requireRoom(Long roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "房间不存在"));
    }

    @Transactional(readOnly = true)
    public boolean canAccessRoom(Long roomId, Long playerId) {
        return isParticipant(requireRoom(roomId), playerId);
    }

    public boolean isParticipant(GameRoom room, Long playerId) {
        return room.getPlayerOne().getId().equals(playerId) || room.getPlayerTwo().getId().equals(playerId);
    }

    private void assertParticipant(GameRoom room, Long playerId) {
        if (!isParticipant(room, playerId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "无权访问该房间");
        }
    }
}
