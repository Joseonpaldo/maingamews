package org.example.websockettest.repository;

import org.example.websockettest.entity.GameRoomEntity;
import org.example.websockettest.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GameRoomRepositoryImpl extends JpaRepository<GameRoomEntity, Long> {


    List<GameRoomEntity> findAllByRoomStatus(Integer roomStatus);

    // roomId로 GameRoomEntity를 가져오는 메소드
    GameRoomEntity findByRoomId(Long roomId);

    //delete
    void deleteByRoomIdAndUser(Long roomId, UserEntity userEntity);

}
