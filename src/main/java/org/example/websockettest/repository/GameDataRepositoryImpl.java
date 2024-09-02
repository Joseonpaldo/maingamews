package org.example.websockettest.repository;

import org.example.websockettest.entity.GameDataEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface GameDataRepositoryImpl extends JpaRepository<GameDataEntity, Long> {
    @Query("SELECT g FROM GameDataEntity g WHERE g.gameRoom.roomId = :roomId")
    List<GameDataEntity> findAllByRoomId(Long roomId);
}
