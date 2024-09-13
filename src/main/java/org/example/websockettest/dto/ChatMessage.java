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
    private String nickname;
    private String session;
    private boolean ready;
    private String invitedUserId;
    private String  receiver;

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
        START_GAME,
        FINISH_RACE,
        UPDATE_SPEED,
        END_GAME,
        INVITE,
        FRIEND_REQUEST,
        DELETE_ROOM
    }

}
