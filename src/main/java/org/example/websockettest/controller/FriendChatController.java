package org.example.websockettest.controller;

import lombok.RequiredArgsConstructor;
import org.example.websockettest.dto.FriendChatMsg;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class FriendChatController {
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/sendFriendMsg")
    public void sendMessage(FriendChatMsg friendChatMsg) {
        String fChatRoomId = (friendChatMsg.getFromId() < friendChatMsg.getToId())
                ? friendChatMsg.getFromId() + "_" + friendChatMsg.getToId()
                : friendChatMsg.getToId() + "_" + friendChatMsg.getFromId();
        friendChatMsg.setFChatRoomId(fChatRoomId);

        String destination = "/user/"+friendChatMsg.getToId()+"queue/messages";
        messagingTemplate.convertAndSendToUser(friendChatMsg.getToId().toString(),destination, friendChatMsg);
    }
}
