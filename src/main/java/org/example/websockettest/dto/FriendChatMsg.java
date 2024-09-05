package org.example.websockettest.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FriendChatMsg {
    private Long fromId;
    private Long toId;
    private String chatMsg;
    private String fChatRoomId;
}
