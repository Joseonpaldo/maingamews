package org.example.websockettest.controller;

import lombok.RequiredArgsConstructor;
import org.example.websockettest.dto.FriendChatMsg;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin("origins = \"http://localhost:3000\"")
@RequiredArgsConstructor
public class FriendChatController {
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/sendFriendMsg")
    public void sendMessage(FriendChatMsg friendChatMsg) {
        String destination = "/user/" + friendChatMsg.getToId() + "/queue/messages";
        messagingTemplate.convertAndSend(destination, friendChatMsg);
    }
}
