package org.example.websockettest.controller;

import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import org.example.websockettest.dto.Location;
import org.example.websockettest.dto.Player;
import org.example.websockettest.dto.SendMessage;
import org.example.websockettest.entity.GameDataEntity;
import org.example.websockettest.repository.GameDataRepositoryImpl;
import org.json.simple.JSONObject;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.*;

@Controller
@RequiredArgsConstructor
public class MainGameController {
    public static Map<String, List<Integer>> yutResult = new HashMap<>();
    final private GameDataRepositoryImpl gameDataRepository;
    private final SimpMessagingTemplate messagingTemplate;
    public Map<String, List<Player>> players = new HashMap<>();
    public Map<String, Integer> currentOrder = new HashMap<>();
    public Map<String, Boolean> currentThrow = new HashMap<>();
    public Map<String, String> sessionRoomId = new HashMap<>();

    public Player player1 = Player.builder()
            .name("플레이어1임")
            .player("player1")
            .avatar("bear")
            .profile("/assets/avatar-1.png")
            .money(5000)
            .location(0)
            .order(1)
            .myTurn(true)
            .build();
    public Player player2 = Player.builder()
            .name("플레이어2임~")
            .player("player2")
            .avatar("panda")
            .profile("/assets/avatar-2.png")
            .money(2311)
            .location(15)
            .order(2)
            .myTurn(false)
            .build();
    public Player player4 = Player.builder()
            .name("플레이어4임~")
            .player("player4")
            .avatar("monkey")
            .profile("/assets/avatar-3.png")
            .money(1555)
            .location(22)
            .order(4)
            .myTurn(false)
            .build();
    public Player player3 = Player.builder()
            .name("플레이어3임~")
            .player("player3")
            .avatar("rabbit")
            .profile("/assets/avatar-6.png")
            .money(5010)
            .location(5)
            .order(3)
            .myTurn(false)
            .build();
    List<Player> playerList = Arrays.asList(player1, player2, player3, player4);


//    @EventListener
//    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
//        System.out.println(event.getMessage().getHeaders().get("simpSessionId"));
//        System.out.println("웹소켓 연결");
//    }

    public static void resultDelete(int index, String roomId) {
        yutResult.get(roomId).remove(index);
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        String sessionId = (String) event.getMessage().getHeaders().get("simpSessionId");
        String roomId = sessionRoomId.get(sessionId);
        try {
            var getPlayerData = players.get(roomId);
            for (Player player : getPlayerData) {
                if (player.getSessionId() != null && player.getSessionId().equals(sessionId)) {
                    player.setSessionId("");
                }
            }
        } catch (NullPointerException e) {
            newPlayerDataPush(roomId);
        }
        var getPlayerData = players.get(roomId);
        for (Player player : getPlayerData) {
            if (player.getSessionId() != null && player.getSessionId().equals(sessionId)) {
                player.setSessionId("");
            }
        }

        sessionRoomId.remove(sessionId);
        sendPlayerInfo(roomId);
    }

    @GetMapping("/web/main/{roomId}")
    public String start(@PathVariable String roomId) {
        System.out.println(roomId);
        messagingTemplate.convertAndSend("/topic/main/start/" + roomId);
        return "ok";
    }

    @MessageMapping("/main/start/{roomId}")
    public void mainGameStartHandle(@DestinationVariable String roomId) {
        newPlayerDataPush(roomId);
    }

    @MessageMapping("/main/join/{roomId}")
    public void handleGameMessage(@DestinationVariable String roomId, @Header String name, @Header String sessionId) {
        sessionRoomId.put(sessionId, roomId);
        var getPlayers = players.get(roomId);
        if (getPlayers == null) {
            SendMessage send = SendMessage.builder().Type("error").Message("not found room").build();
            messagingTemplate.convertAndSend("/topic/main-game/" + roomId, send);
            return;
        }
        for (Player player : getPlayers) {
            if (player.getPlayer().equals(name)) {
                player.setSessionId(sessionId);
                sendPlayerInfo(roomId);
                return;
            }
        }
    }

    @MessageMapping("/main/throwYut/{roomId}")
    public void throwYut(@DestinationVariable String roomId, @Header String name) {
        players.get(roomId).forEach(player -> {
            if (player.isMyTurn() && currentThrow.get(roomId) && player.getPlayer().equals(name)) {
                int random = throwYut();
                if (!yutResult.containsKey(roomId)) {
                    yutResult.put(roomId, new ArrayList<>()); // roomId에 대한 리스트가 없으면 새로 생성
                }
                yutResult.get(roomId).add(random); // random 값을 리스트에 추가
                SendMessage send = SendMessage.builder().Type("getResult").Message(String.valueOf(random)).build();
                messagingTemplate.convertAndSend("/topic/main-game/" + roomId, send);
                currentThrow.put(roomId, false);
                if (random == 4 || random == 5) {
                    currentThrow.put(roomId, true);
                    SendMessage oneMore = SendMessage.builder().Type("commend").Message("oneMore").build();
                    messagingTemplate.convertAndSend("/topic/main-game/" + roomId, oneMore);
                }
                SendMessage results = SendMessage.builder().Type("resultArr").Message(yutResult.get(roomId).toString()).build();
                messagingTemplate.convertAndSend("/topic/main-game/" + roomId, results);
            }
        });
    }

    @MessageMapping("/main/useResult/{roomId}")
    public void useResult(@DestinationVariable String roomId, @Header String name, @Header int item) {
        for (Player player : players.get(roomId)) {
            if (player.isMyTurn() && player.getPlayer().equals(name)) {
                int displayIndex1 = -1;
                int displayIndex2 = -1;
                //중앙에 있을 시
                if (player.getLocation() == 100) {
                    int go1 = (33 + item);
                    displayIndex1 = go1;
                    if (go1 > 36) {
                        displayIndex1 = go1 - 36 + 17;
                    }
                    int go2 = (43 + item);
                    displayIndex2 = go2;
                    if (go2 > 46) {
                        displayIndex2 = go2 - 47;
                    }
                    SendMessage results = SendMessage.builder().Type("displayArrow").Message(Arrays.asList(displayIndex1, displayIndex2).toString()).build();
                    messagingTemplate.convertAndSend("/topic/main-game/" + roomId, results);
                    return;
                }

                if (player.getLocation() == 6) {
                    int go = (item + 29);
                    displayIndex2 = go;
                    if (go == 33) {
                        displayIndex2 = 100;
                    }
                } else if (player.getLocation() == 12) {
                    int go = (item + 39);
                    displayIndex2 = go;
                    if (go == 43) {
                        displayIndex2 = 100;
                    }
                }

                int originIndex = player.getLocation() + item;
                displayIndex1 = originIndex;
                // 크게 한바퀴 돌았을때
                if (originIndex >= 24 && player.getLocation() <= 23) {
                    displayIndex1 -= 24;
                } else if (player.getLocation() >= 30 && player.getLocation() <= 36 && originIndex > 36) {
                    displayIndex1 = originIndex - 37 + 18;
                } else if (player.getLocation() >= 40 && player.getLocation() <= 46 && originIndex > 46) {
                    displayIndex1 = originIndex - 47;
                } else if (originIndex == 33 || originIndex == 43) {
                    // 중앙
                    displayIndex1 = 100;
                }
                SendMessage results = SendMessage.builder().Type("displayArrow").Message(Arrays.asList(displayIndex1, displayIndex2).toString()).build();
                messagingTemplate.convertAndSend("/topic/main-game/" + roomId, results);
            }
        }
    }

    @MessageMapping("/main/arrowClick/{roomId}")
    public void arrowClick(@DestinationVariable String roomId, @Header String name, @Header Integer location, @Header Integer resultDelIndex) {
        resultDelete(resultDelIndex, roomId);
//        System.out.println(yutResult.get(roomId).toString());
        for (Player player : players.get(roomId)) {
            if (player.isMyTurn() && player.getPlayer().equals(name)) {
                player.setLocation(location);

                SendMessage results = SendMessage.builder().Type("resultArr").Message(yutResult.get(roomId).toString()).build();
                messagingTemplate.convertAndSend("/topic/main-game/" + roomId, results);

                sendPlayerInfo(roomId);
            }
        }
    }

    @MessageMapping("/main/buyEstate/{roomId}")
    public void buyState(@DestinationVariable String roomId, @Header String name, @Header Integer location, @Header Integer price) {
        for (Player player : players.get(roomId)) {
            if (player.isMyTurn() && player.getPlayer().equals(name)) {
                List<Location> estateList = player.getEstate();
                if (estateList == null) {
                    estateList = new ArrayList<>(); // estate가 null일 경우 빈 리스트로 초기화
                }
                estateList.add(Location.builder().location(location).landmark(1).build());
                player.setEstate(estateList);
                player.setMoney(player.getMoney() - price);
            }
        }
        sendPlayerInfo(roomId);
    }

    @MessageMapping("/main/upgradeEstate/{roomId}")
    public void upgradeEstate(@DestinationVariable String roomId, @Header String name, @Header Integer location, @Header Integer price) {
        for (Player player : players.get(roomId)) {
            if (player.isMyTurn() && player.getPlayer().equals(name)) {
                for (Location state : player.getEstate()) {
                    if (state.getLocation() == location) {
                        if (state.getLandmark() == 3) {
                            SendMessage results = SendMessage.builder().Type("toast").Message("광역시 이상 등급은 없습니다.").build();
                            messagingTemplate.convertAndSend("/topic/main-game/" + roomId, results);
                            return;
                        }
                        state.setLandmark(state.getLandmark() + 1);
                        player.setMoney(player.getMoney() - price);
                    }
                }
            }
        }
        sendPlayerInfo(roomId);
    }

    @MessageMapping("/main/passTurn/{roomId}")
    public void passTurn(@DestinationVariable String roomId, @Header String name) {
        for (Player player : players.get(roomId)) {
            if (!player.isMyTurn() && player.getPlayer().equals(name)) {
                return;
            }
        }

        int nextTurn = currentOrder.get(roomId) + 1;
        if (nextTurn > 4) {
            nextTurn = 1;
        }

        for (Player player : players.get(roomId)) {
            if (player.getMoney() < 0) {
                nextTurn++;
                if (nextTurn > 4) {
                    nextTurn = 1;
                }
            }
        }
        currentOrder.put(roomId, nextTurn);
        currentThrow.put(roomId, true);
        for (Player player : players.get(roomId)) {
            player.setMyTurn(false);
            if (player.getOrder() == nextTurn) {
                player.setMyTurn(true);
            }
        }
        sendPlayerInfo(roomId);
    }


    public int throwYut() {
        int[] result = {1, 2, 3, 4, 5, 1, 2, 3, 1, 2, 3, 1, 2, 3, 2, 2};
        int rand = (int) (Math.random() * 16);

        return result[rand];
    }

    public void sendPlayerInfo(String roomId) {
        Map<String, Object> jsonMap = new HashMap<>();
        Gson gson = new Gson();
        for (Player player : players.get(roomId)) {
            jsonMap.put("player" + player.getOrder(), gson.toJson(player));
        }
        SendMessage send = SendMessage.builder().Type("getPlayer").Message(JSONObject.toJSONString(jsonMap)).build();
        messagingTemplate.convertAndSend("/topic/main-game/" + roomId, send);
        if (yutResult.get(roomId) != null) {
            SendMessage results = SendMessage.builder().Type("resultArr").Message(yutResult.get(roomId).toString()).build();
            messagingTemplate.convertAndSend("/topic/main-game/" + roomId, results);
        }
        SendMessage isThrow = SendMessage.builder().Type("isThrow").Message(currentThrow.get(roomId).toString()).build();
        messagingTemplate.convertAndSend("/topic/main-game/" + roomId, isThrow);
    }

    public void newPlayerDataPush(String roomId) {
        List<Player> playerList = new ArrayList<>();
        var data = gameDataRepository.findAllByRoomId(Long.valueOf(roomId));
        for (GameDataEntity gameData : data) {
            Player player = Player.builder()
                    .name(gameData.getUser().getNickname())
                    .player("player" + gameData.getMy_turn())
                    .avatar(gameData.getAvatar())
                    .profile(gameData.getUser().getProfilePicture())
                    .money(gameData.getMoney())
                    .location(gameData.getUser_location())
                    .order(gameData.getMy_turn())
                    .myTurn(false)
                    .build();
            if (gameData.getMy_turn() == 1) {
                player.setMyTurn(true);
            }
            playerList.add(player);
        }
        players.put(roomId, playerList);
        currentOrder.put(roomId, 1);
        currentThrow.put(roomId, true);

        System.out.println(playerList);
    }

}


