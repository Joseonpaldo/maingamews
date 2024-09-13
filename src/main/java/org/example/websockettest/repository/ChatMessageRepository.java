package org.example.websockettest.repository;

import org.example.websockettest.entity.ChatMessageEntity;
import org.example.websockettest.entity.ChatRoomEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, Long> {
    // 특정 채팅방의 메시지 목록을 조회하는 메서드
    List<ChatMessageEntity> findByChatRoom(ChatRoomEntity chatRoom);

}
