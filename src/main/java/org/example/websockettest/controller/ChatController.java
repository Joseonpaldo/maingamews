package org.example.websockettest.controller;

import lombok.RequiredArgsConstructor;
import org.example.websockettest.dto.ChatMessage;
import org.example.websockettest.dto.LobbyPlayer;
import org.example.websockettest.entity.GameRoomEntity;
import org.example.websockettest.repository.GameRoomRepositoryImpl;
import org.example.websockettest.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.*;

@Controller
@RequiredArgsConstructor
public class ChatController {
    private static final int MAX_PLAYERS = 4;
    final private GameRoomRepositoryImpl gameRoomRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private Map<String, List<String>> roomPlayers = new HashMap<>();
    private Map<String, Map<String, String>> playerCharacters = new HashMap<>();
    private Map<String, String> roomMaps = new HashMap<>(); // 방별 맵 정보 저장

    private Map<String, List<LobbyPlayer>> roomPlayersData = new HashMap<>();
    private Map<String, LobbyPlayer> sessionByplayerData = new HashMap<>(); //유저 지울때 sender, roomid 필요해서 session으로 저장

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        System.out.println("웹소켓 연결 : " + event.getMessage().getHeaders().get("simpSessionId"));
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        String sessionId = (String) event.getMessage().getHeaders().get("simpSessionId");
        System.out.println("sessionId: " + sessionId);
        LobbyPlayer getPlayer = sessionByplayerData.get(sessionId);
        if (getPlayer == null) {
            System.out.println();
            System.out.println("getPlayer is null");
            System.out.println(sessionByplayerData);
            System.out.println();
            System.out.println();
            System.out.println();
            System.out.println();

            return;
        }
        String roomId = getPlayer.getRoomId();
        String sender = getPlayer.getSender();
        System.out.println("leave " + getPlayer);
        leavePlayer(roomId, sender);
    }


    @MessageMapping("/chat.addUser/{roomId}")
    public void addUser(@DestinationVariable String roomId, ChatMessage chatMessage) {
        System.out.println(chatMessage);
        List<String> playersInRoom = roomPlayers.getOrDefault(roomId, new ArrayList<>());

        if (playersInRoom.size() >= MAX_PLAYERS) {
            ChatMessage errorMessage = ChatMessage.builder()
                    .type(ChatMessage.MessageType.ERROR)
                    .content("Maximum number of players reached")
                    .build();
            messagingTemplate.convertAndSend("/topic/" + roomId, errorMessage);
            return;
        }

        if (playersInRoom.contains(chatMessage.getSender())) {
            return;
        }
        playersInRoom.add(chatMessage.getSender());
        roomPlayers.put(roomId, playersInRoom);

        Map<String, String> charactersInRoom = playerCharacters.getOrDefault(roomId, new HashMap<>());
        String profilePicture = chatMessage.getContent() != null ? chatMessage.getContent() : null;
        charactersInRoom.put(chatMessage.getSender(), profilePicture);
        playerCharacters.put(roomId, charactersInRoom);

        List<LobbyPlayer> getRoomPlayersData = roomPlayersData.getOrDefault(roomId, new ArrayList<>());
        var lobbyPlayerData = LobbyPlayer.builder()
                .nickname(chatMessage.getNickname())
                .sender(chatMessage.getSender())
                .session(chatMessage.getSession())
                .roomId(chatMessage.getRoomId())
                .build();
        getRoomPlayersData.add(lobbyPlayerData);
        roomPlayersData.put(roomId, getRoomPlayersData);

        sessionByplayerData.put(chatMessage.getSession(), lobbyPlayerData);

        String currentMap = roomMaps.getOrDefault(roomId, "/image/map/1.png");
        StringBuilder allPlayersInfo = new StringBuilder();
        for (LobbyPlayer player : getRoomPlayersData) {
            allPlayersInfo.append(player.getSender()).append("|").append(charactersInRoom.get(player.getSender())).append("|").append(player.getNickname()).append(",");
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

        System.out.println(roomPlayers);


        roomCurrentPlayerAdd(roomId);
    }


    @MessageMapping("/chat.leaveUser/{roomId}")
    public void leaveUser(@DestinationVariable String roomId, ChatMessage chatMessage) {
        String sender = chatMessage.getSender();
        leavePlayer(roomId, sender);
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
        System.out.println("chatting : " + chatMessage);
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


//    @GetMapping("/room/{roomId}/status")
//    public ResponseEntity<Map<String, Object>> getRoomStatus(@PathVariable String roomId) {
//        Map<String, Object> roomStatus = new HashMap<>();
//        roomStatus.put("players", roomPlayers.getOrDefault(roomId, new ArrayList<>()));
//        roomStatus.put("characters", playerCharacters.getOrDefault(roomId, new HashMap<>()));
//        roomStatus.put("map", roomMaps.getOrDefault(roomId, "/image/map/1.png"));
//        return ResponseEntity.ok(roomStatus);
//    }

    //    @MessageMapping("/chat.endGame/{roomId}")
//    public void endGame(@DestinationVariable String roomId, ChatMessage chatMessage) {
//        ChatMessage endGameMessage = ChatMessage.builder()
//                .type(ChatMessage.MessageType.END_GAME)
//                .content("The game has ended. Moving to YutPan.")
//                .roomId(roomId)
//                .build();
//        messagingTemplate.convertAndSend("/topic/" + roomId, endGameMessage);
//    }
    public void leavePlayer(String roomId, String sender) {
        List<String> playersInRoom = roomPlayers.getOrDefault(roomId, new ArrayList<>());
        List<LobbyPlayer> lobbyPlayerData = roomPlayersData.get(roomId);

        System.out.println("lobbyPlayerData " + lobbyPlayerData);

// 퇴장 메시지 작성
        ChatMessage leaveMessage = ChatMessage.builder()
                .type(ChatMessage.MessageType.LEAVE)
                .roomId(roomId)
                .build();

        Iterator<LobbyPlayer> iterator = lobbyPlayerData.iterator();
        while (iterator.hasNext()) {
            LobbyPlayer lobbyPlayer = iterator.next();
            if (lobbyPlayer.getSender().equals(sender)) {
                leaveMessage.setContent(lobbyPlayer.getNickname() + " 님이 퇴장하였습니다.");
                iterator.remove(); // 안전하게 요소 제거
            }
        }
        roomPlayersData.put(roomId, lobbyPlayerData);


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

        // 퇴장 메시지 전송
        messagingTemplate.convertAndSend("/topic/" + roomId, leaveMessage);

        // 업데이트된 플레이어 정보 작성
        StringBuilder allPlayersInfo = new StringBuilder();
        for (LobbyPlayer player : lobbyPlayerData) {
            allPlayersInfo.append(player.getSender()).append("|").append(playerCharacters.get(roomId).get(player.getSender())).append("|").append(player.getNickname()).append(",");
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

        roomCurrentPlayerLeave(roomId);
    }

    public void roomCurrentPlayerAdd(String roomId) {
        GameRoomEntity roomData = gameRoomRepository.findById(Long.valueOf(roomId)).get();
        roomData.setCurrPlayer(roomData.getCurrPlayer() + 1);
        gameRoomRepository.save(roomData);
    }

    public void roomCurrentPlayerLeave(String roomId) {
        GameRoomEntity roomData = gameRoomRepository.findById(Long.valueOf(roomId)).get();
        roomData.setCurrPlayer(roomData.getCurrPlayer() - 1);
        gameRoomRepository.save(roomData);
    }

    @Autowired UserService userService;
    //초대하기
    @MessageMapping("/chat.inviteUser/{invitedUser}")
    public void inviteUser(ChatMessage chatMessage) {
        String sender = chatMessage.getSender();
        String invitedUser = chatMessage.getInvitedUserId(); // 초대받는 사용자 ID를 올바른 필드에서 가져옴
        Long userid = Long.parseLong(chatMessage.getSender());
        String nickname = userService.getNicknameByUserIdentifyId(userid);

        if (invitedUser == null || invitedUser.isEmpty()) {
            throw new IllegalArgumentException("Invited user ID must not be null or empty");
        }

        // 초대 메시지 작성 및 전송
        ChatMessage inviteMessage = ChatMessage.builder()
                .type(ChatMessage.MessageType.INVITE)
                .sender(sender)
                .content("You have been invited to join the room.")
                .roomId(chatMessage.getRoomId())  // 방 ID 포함
                .nickname(nickname)
                .build();

// 초대받은 사용자의 ID를 기반으로 메시지 전송
        messagingTemplate.convertAndSend("/topic/" + invitedUser, inviteMessage);

        System.out.println(inviteMessage);
        System.out.println("사용자 " + invitedUser + " 가 초대되었습니다.");
    }



}
