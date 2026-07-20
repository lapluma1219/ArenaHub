package com.arenahub.service;

import com.arenahub.dto.PlayerResponse;
import com.arenahub.dto.RoomDtos.MatchmakingResponse;
import com.arenahub.entity.GameRoom;
import com.arenahub.entity.PlayerProfile;
import com.arenahub.entity.RoomStatus;
import com.arenahub.exception.BusinessException;
import com.arenahub.repository.GameRoomRepository;
import com.arenahub.repository.PlayerProfileRepository;
import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MatchmakingService {
    private final ConcurrentLinkedQueue<Long> waitingPlayers = new ConcurrentLinkedQueue<>();
    private final PlayerProfileRepository playerRepository;
    private final GameRoomRepository roomRepository;

    public MatchmakingService(PlayerProfileRepository playerRepository, GameRoomRepository roomRepository) {
        this.playerRepository = playerRepository;
        this.roomRepository = roomRepository;
    }

    @Transactional
    public synchronized MatchmakingResponse join(Long playerId) {
        Optional<GameRoom> activeRoom = findActiveRoom(playerId);
        if (activeRoom.isPresent()) {
            return matched(activeRoom.get(), playerId);
        }
        removeFromQueue(playerId);
        Long opponentId = pollAvailableOpponent(playerId);
        if (opponentId == null) {
            waitingPlayers.add(playerId);
            return new MatchmakingResponse("WAITING", null, null);
        }

        PlayerProfile player = requirePlayer(playerId);
        PlayerProfile opponent = requirePlayer(opponentId);
        GameRoom room = new GameRoom();
        room.setPlayerOne(opponent);
        room.setPlayerTwo(player);
        GameRoom saved = roomRepository.save(room);
        return new MatchmakingResponse("MATCHED", saved.getId(), PlayerResponse.from(opponent));
    }

    @Transactional(readOnly = true)
    public MatchmakingResponse status(Long playerId) {
        Optional<GameRoom> activeRoom = findActiveRoom(playerId);
        if (activeRoom.isPresent()) {
            return matched(activeRoom.get(), playerId);
        }
        if (waitingPlayers.contains(playerId)) {
            return new MatchmakingResponse("WAITING", null, null);
        }
        return new MatchmakingResponse("IDLE", null, null);
    }

    public synchronized MatchmakingResponse leave(Long playerId) {
        removeFromQueue(playerId);
        return new MatchmakingResponse("IDLE", null, null);
    }

    public boolean isInRoom(GameRoom room, Long playerId) {
        return room.getPlayerOne().getId().equals(playerId) || room.getPlayerTwo().getId().equals(playerId);
    }

    private MatchmakingResponse matched(GameRoom room, Long playerId) {
        PlayerProfile opponent = room.getPlayerOne().getId().equals(playerId) ? room.getPlayerTwo() : room.getPlayerOne();
        return new MatchmakingResponse("MATCHED", room.getId(), PlayerResponse.from(opponent));
    }

    private Optional<GameRoom> findActiveRoom(Long playerId) {
        return roomRepository.findFirstByStatusAndPlayerOneIdOrStatusAndPlayerTwoId(
                RoomStatus.ACTIVE, playerId, RoomStatus.ACTIVE, playerId);
    }

    private Long pollAvailableOpponent(Long playerId) {
        Iterator<Long> iterator = waitingPlayers.iterator();
        while (iterator.hasNext()) {
            Long opponentId = iterator.next();
            iterator.remove();
            if (!opponentId.equals(playerId) && findActiveRoom(opponentId).isEmpty()) {
                return opponentId;
            }
        }
        return null;
    }

    private void removeFromQueue(Long playerId) {
        waitingPlayers.removeIf(playerId::equals);
    }

    private PlayerProfile requirePlayer(Long playerId) {
        return playerRepository.findById(playerId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "玩家不存在"));
    }
}
