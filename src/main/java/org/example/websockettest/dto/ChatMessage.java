package org.example.websockettest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessage {
    private MessageType type;
    private String roomId;
    private String content;
    private String sender;
    private boolean ready;


    public enum MessageType {
        SELECT,
        CHAT,
        JOIN,
        READY,
        LEAVE,
        UPDATE,
        EXISTING_PLAYERS, // 추가
        DESELECT,
        CHANGE_MAP,
        START,
        ERROR,
        HEARTBEAT
    }

}
