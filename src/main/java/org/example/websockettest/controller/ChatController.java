package org.example.websockettest.controller;

import org.example.websockettest.dto.ChatMessage;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.ArrayList;
import java.util.List;

@Controller
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final List<String> players = new ArrayList<>();

    public ChatController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/chat.addUser")
    @SendTo("/topic/public")
    public ChatMessage addUser(ChatMessage chatMessage) {
        if (!players.contains(chatMessage.getSender())) {
            players.add(chatMessage.getSender());
        }


        // 새로 들어온 플레이어에게 현재 모든 플레이어 정보 전달
        ChatMessage newUserMessage = ChatMessage.builder()
                .type(ChatMessage.MessageType.JOIN)
                .content(String.join(",", players))
                .build();

        // 모든 플레이어에게 새로 들어온 플레이어 정보 전달
        messagingTemplate.convertAndSend("/topic/public", newUserMessage);

        System.out.println("Sending existing players: " + newUserMessage.getContent());  // 디버깅 메시지 추가

        return newUserMessage;
    }

    @MessageMapping("/chat.ready")
    @SendTo("/topic/public")
    public ChatMessage readyUser(ChatMessage chatMessage) {
        return chatMessage;
    }

    @MessageMapping("/chat.leaveUser")
    @SendTo("/topic/public")
    public ChatMessage leaveUser(ChatMessage chatMessage) {
        players.remove(chatMessage.getSender());
        return chatMessage;
    }
}
