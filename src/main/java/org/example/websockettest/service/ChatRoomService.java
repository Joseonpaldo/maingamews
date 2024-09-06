package org.example.websockettest.service;

import org.example.websockettest.entity.ChatRoomEntity;
import org.example.websockettest.entity.FriendRelationEntity;
import org.example.websockettest.repository.ChatRoomRepository;
import org.example.websockettest.repository.FriendRelationRepositoryImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ChatRoomService {

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private FriendRelationRepositoryImpl friendRelationRepository;

    // 특정 친구 관계에 기반하여 채팅방을 생성하거나 기존 채팅방을 반환
    public ChatRoomEntity createOrGetChatRoom(Long friendRelationId) {
        FriendRelationEntity friendRelation = friendRelationRepository.findById(friendRelationId)
                .orElseThrow(() -> new IllegalArgumentException("친구 관계를 찾을 수 없습니다."));

        // 해당 친구 관계에 대한 채팅방이 있는지 확인
        Optional<ChatRoomEntity> existingRoom = chatRoomRepository.findByFriendRelation(friendRelation);

        // 이미 채팅방이 있으면 반환, 없으면 새로 생성 후 반환
        if (existingRoom.isPresent()) {
            return existingRoom.get();
        } else {
            ChatRoomEntity newRoom = ChatRoomEntity.builder()
                    .friendRelation(friendRelation)
                    .isActive(true)
                    .build();
            return chatRoomRepository.save(newRoom);
        }
    }

    // ID를 기반으로 채팅방을 조회
    public ChatRoomEntity getChatRoomById(Long roomId) {
        return chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다."));
    }
}
