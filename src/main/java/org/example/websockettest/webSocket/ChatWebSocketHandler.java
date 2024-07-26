package org.example.websockettest.webSocket;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    // 게임 방을 저장할 ConcurrentHashMap
    private static final Map<String, Set<WebSocketSession>> gameRoom = new ConcurrentHashMap<>();

    // 클라이언트가 WebSocket 연결을 성공적으로 맺었을 때 호출되는 메서드
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 세션에서 방 이름을 가져옴
        String room = getRoom(session);
        // 방이 존재하지 않으면 새로 생성하고, 현재 세션을 방에 추가
        gameRoom.computeIfAbsent(room, k -> new CopyOnWriteArraySet<>()).add(session);
        System.out.println("chatRoom : " + gameRoom);
    }

    // 클라이언트로부터 텍스트 메시지를 수신했을 때 호출되는 메서드
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 세션에서 방 이름을 가져옴
        String room = getRoom(session);
        JSONParser parser = new JSONParser();
        JSONObject jsonObject = (JSONObject) parser.parse(message.getPayload());
        System.out.println("chat : " + jsonObject);

        // 해당 방의 모든 세션에 메시지를 전송
        for (WebSocketSession webSocketSession : gameRoom.get(room)) {
            // 세션이 열려 있는 경우에만 메시지를 전송
            if (webSocketSession.isOpen()) {
                String mo = message.getPayload(); // 수신한 메시지 내용
                webSocketSession.sendMessage(new TextMessage(mo)); // 메시지 전송
            }
        }
    }

    // 클라이언트가 WebSocket 연결을 종료했을 때 호출되는 메서드
    public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status)
            throws Exception {
        // 세션에서 방 이름을 가져옴
        String room = getRoom(session);
        // 해당 방에서 세션을 제거
        gameRoom.getOrDefault(room, new CopyOnWriteArraySet<>()).remove(session);
        System.out.println("closed : " + room);
    }

    // WebSocket 세션에서 방 이름을 추출하는 메서드
    public String getRoom(WebSocketSession session) {
        // URI 경로에서 방 이름을 추출하여 반환
        return session.getUri().getPath().split("/")[2];
    }
}
