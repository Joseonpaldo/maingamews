package org.example.websockettest.controller;

import org.example.websockettest.dto.ChatMessage;
import org.example.websockettest.dto.ChattingDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class ChattingController {

    // 메시지를 특정 사용자에게 보내기 위한 템플릿
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // 클라이언트가 메시지를 전송할 때 사용하는 경로 ("/app/chat.sendMessage")
    @MessageMapping("/chat.sendMessage")
    public void sendMessage(ChattingDto chattingDto) {
        // 특정 사용자에게 메시지 전송
        messagingTemplate.convertAndSendToUser(
                chattingDto.getReceiverId().toString(), // 수신자 ID
                "/queue/messages", // 해당 사용자에게 메시지를 보낼 경로
                chattingDto // 전송할 메시지
        );
    }
}
