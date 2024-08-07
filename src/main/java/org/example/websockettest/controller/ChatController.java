package org.example.websockettest.controller;

import org.example.websockettest.dto.ChatMessage;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private static final int MAX_PLAYERS = 4;
    private static final long TIMEOUT = 5000; // heart 초
    private Map<String, List<String>> roomPlayers = new HashMap<>();
    private Map<String, Map<String, String>> playerCharacters = new HashMap<>();
    private Map<String, String> roomMaps = new HashMap<>(); // 방별 맵 정보 저장
    private Map<String, Long> playerHeartbeat = new ConcurrentHashMap<>();


    public ChatController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;

        // Heartbeat 체크 쓰레드
        new Thread(() -> {
            while (true) {
                long currentTime = System.currentTimeMillis();
                playerHeartbeat.forEach((player, lastHeartbeat) -> {
                    if (currentTime - lastHeartbeat > TIMEOUT) {
                        String roomId = getRoomIdByPlayer(player);
                        if (roomId != null) {
                            leaveUser(roomId, ChatMessage.builder()
                                    .sender(player)
                                    .type(ChatMessage.MessageType.LEAVE)
                                    .roomId(roomId)
                                    .build());
                        }
                    }
                });
                try {
                    Thread.sleep(5000); // 5초마다 체크
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    @MessageMapping("/chat.addUser/{roomId}")
    public void addUser(@DestinationVariable String roomId, ChatMessage chatMessage) {
        List<String> playersInRoom = roomPlayers.getOrDefault(roomId, new ArrayList<>());

        if (playersInRoom.size() >= MAX_PLAYERS) {
            ChatMessage errorMessage = ChatMessage.builder()
                    .type(ChatMessage.MessageType.ERROR)
                    .content("Maximum number of players reached")
                    .build();
            messagingTemplate.convertAndSend("/topic/" + roomId, errorMessage);
            return;
        }

        if (!playersInRoom.contains(chatMessage.getSender())) {
            playersInRoom.add(chatMessage.getSender());
            roomPlayers.put(roomId, playersInRoom);

            Map<String, String> charactersInRoom = playerCharacters.getOrDefault(roomId, new HashMap<>());
            charactersInRoom.put(chatMessage.getSender(), "/image/pinkbin.png");
            playerCharacters.put(roomId, charactersInRoom);
        }

        // 맵 정보 포함하여 전송
        String currentMap = roomMaps.getOrDefault(roomId, "/image/map/1.png");

        ChatMessage joinMessage = ChatMessage.builder()
                .type(ChatMessage.MessageType.JOIN)
                .content(chatMessage.getSender() + " 님이 입장하였습니다.")
                .roomId(roomId)
                .build();

        messagingTemplate.convertAndSend("/topic/" + roomId, joinMessage);

        // 기존 플레이어 정보 및 맵 정보 전송
        Map<String, String> charactersInRoom = playerCharacters.get(roomId);
        StringBuilder allPlayersInfo = new StringBuilder();
        for (String player : playersInRoom) {
            allPlayersInfo.append(player).append(":").append(charactersInRoom.get(player)).append(",");
        }

        if (allPlayersInfo.length() > 0) {
            allPlayersInfo.deleteCharAt(allPlayersInfo.length() - 1); // 마지막 콤마 제거
        }

        ChatMessage newUserMessage = ChatMessage.builder()
                .type(ChatMessage.MessageType.UPDATE)
                .content(allPlayersInfo.toString())
                .roomId(roomId)
                .build();

        messagingTemplate.convertAndSend("/topic/" + roomId, newUserMessage);

        // 맵 정보 전송
        ChatMessage mapMessage = ChatMessage.builder()
                .type(ChatMessage.MessageType.CHANGE_MAP)
                .content(currentMap)
                .roomId(roomId)
                .build();

        messagingTemplate.convertAndSend("/topic/" + roomId, mapMessage);

        // 현재 선택된 캐릭터 목록 전송
        ChatMessage selectedCharactersMessage = ChatMessage.builder()
                .type(ChatMessage.MessageType.UPDATE)
                .content(allPlayersInfo.toString())
                .roomId(roomId)
                .build();

        messagingTemplate.convertAndSend("/topic/" + roomId, selectedCharactersMessage);

        System.out.println("room number: " + newUserMessage.getRoomId());
        System.out.println("Sending existing players: " + newUserMessage.getContent());
    }


    @MessageMapping("/chat.leaveUser/{roomId}")
    public void leaveUser(@DestinationVariable String roomId, ChatMessage chatMessage) {
        String sender = chatMessage.getSender();
        List<String> playersInRoom = roomPlayers.getOrDefault(roomId, new ArrayList<>());

        if (playersInRoom.contains(sender)) {
            playersInRoom.remove(sender);
            roomPlayers.put(roomId, playersInRoom);
        }

        // 플레이어 캐릭터 초기화
        if (playerCharacters.containsKey(roomId)) {
            Map<String, String> charactersInRoom = playerCharacters.get(roomId);
            charactersInRoom.remove(sender);
            playerCharacters.put(roomId, charactersInRoom);
        }

        // 퇴장 메시지 작성
        ChatMessage leaveMessage = ChatMessage.builder()
                .type(ChatMessage.MessageType.LEAVE)
                .content(sender + " 님이 퇴장하였습니다.")
                .roomId(roomId)
                .build();

        // 퇴장 메시지 전송
        messagingTemplate.convertAndSend("/topic/" + roomId, leaveMessage);

        // 업데이트된 플레이어 정보 작성
        StringBuilder allPlayersInfo = new StringBuilder();
        for (String player : playersInRoom) {
            allPlayersInfo.append(player).append(":").append(playerCharacters.get(roomId).get(player)).append(",");
        }

        if (allPlayersInfo.length() > 0) {
            allPlayersInfo.deleteCharAt(allPlayersInfo.length() - 1);
        }

        ChatMessage updateUserMessage = ChatMessage.builder()
                .type(ChatMessage.MessageType.UPDATE)
                .content(allPlayersInfo.toString())
                .roomId(roomId)
                .build();

        // 업데이트된 플레이어 정보 전송
        messagingTemplate.convertAndSend("/topic/" + roomId, updateUserMessage);

        System.out.println("Sending existing players: " + String.join(",", playersInRoom));
    }


    @MessageMapping("/chat.heartbeat/{roomId}")
    public void receiveHeartbeat(@DestinationVariable String roomId, ChatMessage chatMessage) {
        playerHeartbeat.put(chatMessage.getSender(), System.currentTimeMillis());
    }

    @MessageMapping("/chat.ready/{roomId}")
    public void readyUser(@DestinationVariable String roomId, ChatMessage chatMessage) {
        messagingTemplate.convertAndSend("/topic/" + roomId, chatMessage);
    }

    @MessageMapping("/chat.selectCharacter/{roomId}")
    public void selectCharacter(@DestinationVariable String roomId, ChatMessage chatMessage) {
        String sender = chatMessage.getSender();
        String characterSrc = chatMessage.getContent();

        if (playerCharacters.containsKey(roomId)) {
            Map<String, String> charactersInRoom = playerCharacters.get(roomId);

            // 이미 선택된 캐릭터인지 확인
            if (charactersInRoom.containsValue(characterSrc)) {
                ChatMessage errorMessage = ChatMessage.builder()
                        .type(ChatMessage.MessageType.ERROR)
                        .sender(sender)
                        .content("Character already selected")
                        .roomId(roomId)
                        .build();

                System.out.println("error message: " + errorMessage);

                messagingTemplate.convertAndSend("/topic/" + roomId, errorMessage);
                return;
            }

            charactersInRoom.put(sender, characterSrc);
            playerCharacters.put(roomId, charactersInRoom);

            ChatMessage characterSelectMessage = ChatMessage.builder()
                    .type(ChatMessage.MessageType.SELECT)
                    .sender(sender)
                    .content(characterSrc)
                    .roomId(roomId)
                    .build();

            System.out.println("character selected: " + characterSelectMessage);
            messagingTemplate.convertAndSend("/topic/" + roomId, characterSelectMessage);
        }
    }

    @MessageMapping("/chat.deselectCharacter/{roomId}")
    public void deselectCharacter(@DestinationVariable String roomId, ChatMessage chatMessage) {
        String sender = chatMessage.getSender();
        String characterSrc = chatMessage.getContent();

        if (playerCharacters.containsKey(roomId)) {
            Map<String, String> charactersInRoom = playerCharacters.get(roomId);
            if (charactersInRoom.get(sender).equals(characterSrc)) {
                charactersInRoom.put(sender, "/image/pinkbin.png"); // 기본 캐릭터로 설정
                playerCharacters.put(roomId, charactersInRoom);

                ChatMessage characterDeselectMessage = ChatMessage.builder()
                        .type(ChatMessage.MessageType.DESELECT)
                        .sender(sender)
                        .content(characterSrc) // 캐릭터 이미지 경로 포함
                        .roomId(roomId)
                        .build();

                System.out.println("deselect: " + characterDeselectMessage);
                messagingTemplate.convertAndSend("/topic/" + roomId, characterDeselectMessage);
            }
        }
    }

    @MessageMapping("/chat.sendMessage/{roomId}")
    public void sendMessage(@DestinationVariable String roomId, ChatMessage chatMessage) {
        System.out.println("chatMessage: " + chatMessage);
        messagingTemplate.convertAndSend("/topic/" + roomId, chatMessage);
    }

    @MessageMapping("/chat.changeMap/{roomId}")
    public void changeMap(@DestinationVariable String roomId, ChatMessage chatMessage) {
        roomMaps.put(roomId, chatMessage.getContent());

        ChatMessage mapMessage = ChatMessage.builder()
                .type(ChatMessage.MessageType.CHANGE_MAP)
                .content(chatMessage.getContent())
                .roomId(roomId)
                .build();

        messagingTemplate.convertAndSend("/topic/" + roomId, mapMessage);

        System.out.println("Map changed for room: " + roomId + " to " + chatMessage.getContent());
    }

    @MessageMapping("/chat.startGame/{roomId}")
    public void startGame(@DestinationVariable String roomId, ChatMessage chatMessage) {
        List<String> playersInRoom = roomPlayers.getOrDefault(roomId, new ArrayList<>());
        Map<String, String> charactersInRoom = playerCharacters.getOrDefault(roomId, new HashMap<>());
        String currentMap = roomMaps.getOrDefault(roomId, "/image/map/1.png");

        if (playersInRoom.size() == 0) {
            ChatMessage errorMessage = ChatMessage.builder()
                    .type(ChatMessage.MessageType.ERROR)
                    .content("No players in the room")
                    .roomId(roomId)
                    .build();
            messagingTemplate.convertAndSend("/topic/" + roomId, errorMessage);
            return;
        }

        // 게임 시작 정보 생성
        StringBuilder gameInfo = new StringBuilder();
        gameInfo.append("Room ID: ").append(roomId).append("\n");
        gameInfo.append("Map: ").append(currentMap).append("\n");
        gameInfo.append("Players:\n");

        for (String player : playersInRoom) {
            gameInfo.append(player).append(": ").append(charactersInRoom.get(player)).append("\n");
        }

        // 게임 시작 메시지 생성 및 전송
        ChatMessage startMessage = ChatMessage.builder()
                .type(ChatMessage.MessageType.START)
                .content(gameInfo.toString())
                .roomId(roomId)
                .build();

        messagingTemplate.convertAndSend("/topic/" + roomId, startMessage);

        System.out.println("Game started for room: " + roomId);
        System.out.println("Game info: " + gameInfo.toString());
    }

    @GetMapping("/room/{roomId}/status")
    public ResponseEntity<Map<String, Object>> getRoomStatus(@PathVariable String roomId) {
        Map<String, Object> roomStatus = new HashMap<>();
        roomStatus.put("players", roomPlayers.getOrDefault(roomId, new ArrayList<>()));
        roomStatus.put("characters", playerCharacters.getOrDefault(roomId, new HashMap<>()));
        roomStatus.put("map", roomMaps.getOrDefault(roomId, "/image/map/1.png"));
        return ResponseEntity.ok(roomStatus);
    }

    private String getRoomIdByPlayer(String playerName) {
        for (Map.Entry<String, List<String>> entry : roomPlayers.entrySet()) {
            if (entry.getValue().contains(playerName)) {
                return entry.getKey();
            }
        }
        return null;
    }
}
