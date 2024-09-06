package org.example.websockettest.controller;

import org.example.websockettest.dto.ChattingDto;
import org.example.websockettest.entity.ChatMessageEntity;
import org.example.websockettest.entity.ChatRoomEntity;
import org.example.websockettest.service.ChatMessageService;
import org.example.websockettest.service.ChatRoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.socket.messaging.SessionConnectedEvent;

import java.util.List;

@RestController
public class ChattingController {

    @Autowired
    private ChatMessageService chatMessageService;

    @Autowired
    private ChatRoomService chatRoomService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        System.out.println("WebSocket 연결된 세션 ID : " + event.getMessage().getHeaders().get("simpSessionId"));
        System.out.println("연결된 사용자 정보 : " + event.getUser());  // WebSocket 연결된 사용자 정보 출력
    }

    /**
     * 클라이언트에서 메시지를 전송할 때 호출되는 메서드
     * 메시지를 DB에 저장하고 수신자에게 전송합니다.
     */
    @MessageMapping("/chat/sendMessage/{userId}")
    public void sendMessage(ChattingDto chatMessageDto) {
        // 채팅방 조회 또는 생성
        ChatRoomEntity chatRoom = chatRoomService.createOrGetChatRoom(chatMessageDto.getFriendRelationId());

        // 메시지를 DB에 저장
        ChatMessageEntity savedMessage = chatMessageService.saveMessage(
                chatRoom,
                chatMessageDto.getSenderId(),  // UserEntity로 변환해야 함
                chatMessageDto.getMessageContent()
        );

        // 수신자에게 메시지 전송
        messagingTemplate.convertAndSendToUser(
                chatMessageDto.getReceiverId().toString(),  // 수신자의 ID를 사용한 경로
                "/queue/messages",  // 수신자가 구독하는 경로
                savedMessage  // 전송할 메시지
        );

        System.out.println("대화 저장: " + savedMessage);
    }


    /**
     * 특정 채팅방의 모든 메시지를 조회하는 메서드
     * 클라이언트가 채팅방에 들어올 때 이전 메시지를 불러오는 데 사용됩니다.
     */
    @GetMapping("/room/{roomId}/messages")
    public List<ChatMessageEntity> getMessages(@PathVariable Long roomId) {
        // 채팅방 ID로 채팅방을 조회
        ChatRoomEntity chatRoom = chatRoomService.getChatRoomById(roomId);
        // 해당 채팅방의 모든 메시지를 반환

        System.out.println("chatroomw" + chatRoom);
        return chatMessageService.getMessagesByChatRoom(chatRoom);
    }
}
