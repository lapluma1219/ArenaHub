package com.arenahub.repository;

import com.arenahub.entity.GameRoom;
import com.arenahub.entity.RoomStatus;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GameRoomRepository extends JpaRepository<GameRoom, Long> {
    Optional<GameRoom> findFirstByStatusAndPlayerOneIdOrStatusAndPlayerTwoId(
            RoomStatus statusOne, Long playerOneId, RoomStatus statusTwo, Long playerTwoId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select room from GameRoom room where room.id = :roomId")
    Optional<GameRoom> findByIdForUpdate(@Param("roomId") Long roomId);
}
