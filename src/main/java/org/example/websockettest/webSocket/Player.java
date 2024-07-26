package org.example.websockettest.webSocket;

import lombok.Builder;
import lombok.Data;
import org.springframework.web.socket.WebSocketSession;

@Data
@Builder
public class Player {
    private String name;
    private WebSocketSession webSocketSession;
    private int order;
}
