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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

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
            String profilePicture = chatMessage.getContent() != null ? chatMessage.getContent() : null;
            charactersInRoom.put(chatMessage.getSender(), profilePicture);
            playerCharacters.put(roomId, charactersInRoom);
        }

        String currentMap = roomMaps.getOrDefault(roomId, "/image/map/1.png");
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

        ChatMessage mapMessage = ChatMessage.builder()
                .type(ChatMessage.MessageType.CHANGE_MAP)
                .content(currentMap)
                .roomId(roomId)
                .build();

        messagingTemplate.convertAndSend("/topic/" + roomId, mapMessage);
    }

    @MessageMapping("/chat.leaveUser/{roomId}")
    public void leaveUser(@DestinationVariable String roomId, ChatMessage chatMessage) {
        String sender = chatMessage.getSender();
        List<String> playersInRoom = roomPlayers.getOrDefault(roomId, new ArrayList<>());

        if (playersInRoom.contains(sender)) {
            playersInRoom.remove(sender);
            roomPlayers.put(roomId, playersInRoom);
        }

        if (playerCharacters.containsKey(roomId)) {
            Map<String, String> charactersInRoom = playerCharacters.get(roomId);
            if (charactersInRoom != null) {
                charactersInRoom.remove(sender);
                playerCharacters.put(roomId, charactersInRoom);
            }
        }

        ChatMessage leaveMessage = ChatMessage.builder()
                .type(ChatMessage.MessageType.LEAVE)
                .content(sender + " 님이 퇴장하였습니다.")
                .roomId(roomId)
                .build();

        messagingTemplate.convertAndSend("/topic/" + roomId, leaveMessage);

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

        messagingTemplate.convertAndSend("/topic/" + roomId, updateUserMessage);
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

            if (charactersInRoom.containsValue(characterSrc)) {
                ChatMessage errorMessage = ChatMessage.builder()
                        .type(ChatMessage.MessageType.ERROR)
                        .sender(sender)
                        .content("Character already selected")
                        .roomId(roomId)
                        .build();

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

            messagingTemplate.convertAndSend("/topic/" + roomId, characterSelectMessage);
        }
    }

    @MessageMapping("/chat.deselectCharacter/{roomId}")
    public void deselectCharacter(@DestinationVariable String roomId, ChatMessage chatMessage) {
        String sender = chatMessage.getSender();
        String characterSrc = chatMessage.getContent();

        if (playerCharacters.containsKey(roomId)) {
            Map<String, String> charactersInRoom = playerCharacters.get(roomId);
            charactersInRoom.put(sender, characterSrc);
            playerCharacters.put(roomId, charactersInRoom);

            ChatMessage characterDeselectMessage = ChatMessage.builder()
                    .type(ChatMessage.MessageType.DESELECT)
                    .sender(sender)
                    .content(characterSrc)
                    .roomId(roomId)
                    .build();

            messagingTemplate.convertAndSend("/topic/" + roomId, characterDeselectMessage);
        }
    }

    @MessageMapping("/chat.sendMessage/{roomId}")
    public void sendMessage(@DestinationVariable String roomId, ChatMessage chatMessage) {
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
    }

    @MessageMapping("/chat.startGame/{roomId}")
    public void startGame(@DestinationVariable String roomId, ChatMessage chatMessage) {
        List<String> playersInRoom = roomPlayers.getOrDefault(roomId, new ArrayList<>());
        Map<String, String> charactersInRoom = playerCharacters.getOrDefault(roomId, new HashMap<>());
        String currentMap = roomMaps.getOrDefault(roomId, "/image/map/1.png");
        if (playersInRoom.isEmpty()) {
            ChatMessage errorMessage = ChatMessage.builder()
                    .type(ChatMessage.MessageType.ERROR)
                    .content("No players in the room")
                    .roomId(roomId)
                    .build();
            messagingTemplate.convertAndSend("/topic/" + roomId, errorMessage);
            return;
        }
        // 각 플레이어에게 랜덤 속도 할당
        Map<String, Integer> playerSpeeds = new HashMap<>();
        Random random = new Random();
        for (String player : playersInRoom) {
            playerSpeeds.put(player, random.nextInt(30) + 1); // 1~10 사이의 속도
            playerSpeeds.put(player, random.nextInt(30) + 1); // 1~30 사이의 속도
        }

        // 서버에서 계산된 순위 정보를 포함하여 클라이언트로 전송
        List<Map.Entry<String, Integer>> sortedPlayers = playerSpeeds.entrySet()
                .stream()
                .sorted((entry1, entry2) -> entry2.getValue().compareTo(entry1.getValue())) // 역순으로 정렬
                .collect(Collectors.toList());
        StringBuilder gameInfo = new StringBuilder();
        gameInfo.append("Room ID: ").append(roomId).append("\n");
        gameInfo.append("Map: ").append(currentMap).append("\n");
        gameInfo.append("Players:\n");
        int rank = 1;
        for (Map.Entry<String, Integer> entry : sortedPlayers) {
            gameInfo.append(rank).append(". ").append(entry.getKey())
                    .append(": ").append(charactersInRoom.get(entry.getKey()))
                    .append(", Speed: ").append(entry.getValue()).append("\n");
            rank++;
        }
        ChatMessage startMessage = ChatMessage.builder()
                .type(ChatMessage.MessageType.START)
                .content(gameInfo.toString())
                .roomId(roomId)
                .build();

        System.out.println(startMessage);
        messagingTemplate.convertAndSend("/topic/" + roomId, startMessage);
    }


    @GetMapping("/room/{roomId}/status")
    public ResponseEntity<Map<String, Object>> getRoomStatus(@PathVariable String roomId) {
        Map<String, Object> roomStatus = new HashMap<>();
        roomStatus.put("players", roomPlayers.getOrDefault(roomId, new ArrayList<>()));
        roomStatus.put("characters", playerCharacters.getOrDefault(roomId, new HashMap<>()));
        roomStatus.put("map", roomMaps.getOrDefault(roomId, "/image/map/1.png"));
        return ResponseEntity.ok(roomStatus);
    }

//    @MessageMapping("/chat.endGame/{roomId}")
//    public void endGame(@DestinationVariable String roomId, ChatMessage chatMessage) {
//        ChatMessage endGameMessage = ChatMessage.builder()
//                .type(ChatMessage.MessageType.END_GAME)
//                .content("The game has ended. Moving to YutPan.")
//                .roomId(roomId)
//                .build();
//        messagingTemplate.convertAndSend("/topic/" + roomId, endGameMessage);
//    }
}
