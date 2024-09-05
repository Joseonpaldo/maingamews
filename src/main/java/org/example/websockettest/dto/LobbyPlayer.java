package org.example.websockettest.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LobbyPlayer {
    private String roomId;
    private String sender;
    private String nickname;
    private String session;
    private String avatar;
    private int order;
}
