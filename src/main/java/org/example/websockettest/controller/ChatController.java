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
    private Map<String, List<String>> roomPlayers = new HashMap<>();
    private Map<String, Map<String, String>> playerCharacters = new HashMap<>();
    private Map<String, String> roomMaps = new HashMap<>(); // 방별 맵 정보 저장

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
            playerSpeeds.put(player, random.nextInt(30) + 1); // 1~30 사이의 속도
        }

        // 서버에서 순위를 정하지 않고 속도 정보만 클라이언트로 전송
        StringBuilder gameInfo = new StringBuilder();
        gameInfo.append("Room ID: ").append(roomId).append("\n");
        gameInfo.append("Map: ").append(currentMap).append("\n");
        gameInfo.append("Players:\n");
        for (Map.Entry<String, Integer> entry : playerSpeeds.entrySet()) {
            gameInfo.append(entry.getKey())
                    .append(": ").append(charactersInRoom.get(entry.getKey()))
                    .append(", Speed: ").append(entry.getValue()).append("\n");
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


    @MessageMapping("/chat.updateRank/{roomId}")
    public void updateRank(@DestinationVariable String roomId, List<Map<String, Object>> rankings) {

        rankings.forEach(rankInfo -> {
            String player = (String) rankInfo.get("name");
            Integer userId = (Integer) rankInfo.get("userId");
            Integer rank = (Integer) rankInfo.get("rank");
            System.out.println("Player: " + player + ", Rank: " + rank + ", UserId: " + userId);
        });

        // 필요시 전체 순위 정보를 다시 클라이언트에 전송할 수 있습니다.
        ChatMessage rankUpdateMessage = ChatMessage.builder()
                .type(ChatMessage.MessageType.RANK_UPDATE)
                .content(rankings.toString()) // 또는 필요한 형식으로 변환
                .roomId(roomId)
                .build();

        messagingTemplate.convertAndSend("/topic/" + roomId, rankUpdateMessage);
    }



}
