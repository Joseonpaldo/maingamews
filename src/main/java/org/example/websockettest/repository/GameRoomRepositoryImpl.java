package org.example.websockettest.repository;

import org.example.websockettest.entity.GameRoomEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GameRoomRepositoryImpl extends JpaRepository<GameRoomEntity, Long> {
    List<GameRoomEntity> findAllByRoomStatus(Integer roomStatus);
}
