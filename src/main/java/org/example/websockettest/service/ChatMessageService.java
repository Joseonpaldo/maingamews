package org.example.websockettest.service;

import jakarta.transaction.Transactional;
import org.example.websockettest.entity.ChatMessageEntity;
import org.example.websockettest.entity.ChatRoomEntity;
import org.example.websockettest.entity.UserEntity;
import org.example.websockettest.repository.ChatMessageRepository;
import org.example.websockettest.repository.UserRepositoryImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ChatMessageService {

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private UserRepositoryImpl userRepository;  // UserEntity를 가져오기 위한 Repository 추가

    // 메시지 저장 서비스
    @Transactional
    public ChatMessageEntity saveMessage(ChatRoomEntity chatRoom, Long senderId, String messageContent) {
        // senderId로 UserEntity 조회
        UserEntity sender = userRepository.findById(senderId)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다. senderId: " + senderId));

        ChatMessageEntity message = ChatMessageEntity.builder()
                .chatRoom(chatRoom)
                .sender(sender)
                .messageContent(messageContent)
                .sentAt(LocalDateTime.now())
                .build();

        return chatMessageRepository.save(message);
    }


    // 특정 채팅방의 모든 메시지 불러오기
    public List<ChatMessageEntity> getMessagesByChatRoom(ChatRoomEntity chatRoom) {
        return chatMessageRepository.findByChatRoom(chatRoom);
    }
}

