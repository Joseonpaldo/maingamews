package org.example.websockettest.controller;

import org.example.websockettest.dto.ChattingDto;
import org.example.websockettest.entity.ChatMessageEntity;
import org.example.websockettest.entity.ChatRoomEntity;
import org.example.websockettest.service.ChatMessageService;
import org.example.websockettest.service.ChatRoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.socket.messaging.SessionConnectedEvent;

import java.util.List;
import java.util.Map;

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
    }

    /**
     * 클라이언트에서 메시지를 전송할 때 호출되는 메서드
     * 메시지를 DB에 저장하고 수신자에게 전송합니다.
     */
    @MessageMapping("/chat/sendMessage")
    public void sendMessage(ChattingDto chatMessageDto) {
        try {
            // Null 값 체크
            if (chatMessageDto.getSenderId() == null || chatMessageDto.getReceiverId() == null || chatMessageDto.getUserId1() == null || chatMessageDto.getUserId2() == null) {
                throw new IllegalArgumentException("필수 값이 누락되었습니다. senderId, receiverId, 또는 userId1, userId2가 null입니다.");
            }

            // userId1과 userId2로 친구 관계를 확인하여 채팅방을 생성하거나 가져옵니다.
            ChatRoomEntity chatRoom = chatRoomService.createOrGetChatRoom(chatMessageDto.getUserId1(), chatMessageDto.getUserId2());

            // 메시지를 저장합니다.
            ChatMessageEntity savedMessage = chatMessageService.saveMessage(
                    chatRoom,
                    chatMessageDto.getSenderId(),
                    chatMessageDto.getMessageContent()
            );

            // 메시지를 수신자에게 전송합니다.
            messagingTemplate.convertAndSendToUser(
                    chatMessageDto.getReceiverId().toString(),
                    "/queue/messages",
                    savedMessage
            );

            System.out.println("대화 저장: " + savedMessage);
        } catch (Exception e) {
            System.err.println("메시지 전송 오류: " + e.getMessage());
        }
    }



}