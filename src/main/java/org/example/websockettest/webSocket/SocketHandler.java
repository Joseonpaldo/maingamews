package org.example.websockettest.webSocket;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
public class SocketHandler extends TextWebSocketHandler {

    // 게임 방을 저장할 ConcurrentHashMap
    private static final Map<String, List<Player>> gameRoom = new ConcurrentHashMap<>();

    // 클라이언트가 WebSocket 연결을 성공적으로 맺었을 때 호출되는 메서드
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 세션에서 방 이름을 가져옴
        String room = getRoom(session);

        // 방이 존재하지 않으면 새로 생성하고, 현재 세션을 방에 추가
        gameRoom.putIfAbsent(room, new ArrayList<>());

// 플레이어 이름을 세션 ID로 사용 (예시)
        int order = gameRoom.get(room).size(); // 현재 방의 플레이어 수를 기반으로 순서 결정
        String playerName = "player" + (order + 1);

        // 새 플레이어 추가
        gameRoom.get(room).add(new Player(playerName, session, order));

        // 방의 모든 세션에 메시지 전송
        for (Player player : gameRoom.get(room)) {
            if (player.getWebSocketSession().isOpen()) {
                String mo = playerName + "님 접속";
                player.getWebSocketSession().sendMessage(new TextMessage(makeJSONMessage(mo).toJSONString()));
            }
        }

    }

    // 클라이언트로부터 텍스트 메시지를 수신했을 때 호출되는 메서드
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String room = getRoom(session);
        JSONParser parser = new JSONParser();
        JSONObject jsonObject = (JSONObject) parser.parse(message.getPayload());
        System.out.println("command : " + jsonObject);

        int result = 0;
        if (jsonObject.get("command").toString().equals("ThrowYut")) {
            result = throwYut();
        }

        // 해당 방의 모든 플레이어에게 메시지를 전송
        for (Player player : gameRoom.get(room)) {
            if (player.getWebSocketSession().isOpen()) {
                String mo = player.getName() + "의 결과: " + result; // 플레이어 이름 사용
                player.getWebSocketSession().sendMessage(new TextMessage(makeJSONCommend(mo).toJSONString()));
            }
        }
    }


    // 클라이언트가 WebSocket 연결을 종료했을 때 호출되는 메서드
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String room = getRoom(session);

        // 해당 방에서 플레이어 제거
        List<Player> players = gameRoom.get(room);
        if (players != null) {
            players.removeIf(player -> player.getWebSocketSession().getId().equals(session.getId())); // 세션 ID로 플레이어 찾기
        }
        System.out.println("closed : " + room);
    }



    // WebSocket 세션에서 방 이름을 추출하는 메서드
    public String getRoom(WebSocketSession session) {
        // URI 경로에서 방 이름을 추출하여 반환
        return Objects.requireNonNull(session.getUri()).getPath().split("/")[2];
    }

    public int throwYut() {
        int[] result = {1, 2, 3, 4, 5, 1, 2, 3, 1, 2, 3, 1, 2, 3, 2, 2};
        int rand = (int) (Math.random() * 16);

        return result[rand];
    }

    public JSONObject makeJSONMessage(String msg) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("msg", msg);
        jsonObject.put("type", "message");
        return jsonObject;
    }

    public JSONObject makeJSONCommend(String msg) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("command", msg);
        jsonObject.put("type", "command");
        return jsonObject;
    }


}

