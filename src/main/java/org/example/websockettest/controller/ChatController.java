package org.example.websockettest.controller;

import org.example.websockettest.dto.ChatMessage;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private static final int MAX_PLAYERS = 4;
    private Map<String, List<String>> roomPlayers = new HashMap<>(); // roomPlayers 맵 선언 및 초기화
    private Map<String, Map<String, String>> playerCharacters = new HashMap<>(); // roomId -> (playerName -> characterSrc)

    public ChatController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    //로비 입장
    @MessageMapping("/chat.addUser")
    @SendTo("/topic/public")
    public ChatMessage addUser(ChatMessage chatMessage) {
        String roomId = chatMessage.getRoomId();
        List<String> playersInRoom = roomPlayers.getOrDefault(roomId, new ArrayList<>());

        if (playersInRoom.size() >= MAX_PLAYERS) {
            ChatMessage errorMessage = ChatMessage.builder()
                    .type(ChatMessage.MessageType.ERROR)
                    .content("Maximum number of players reached")
                    .build();
            return errorMessage;
        }

        if (!playersInRoom.contains(chatMessage.getSender())) {
            playersInRoom.add(chatMessage.getSender());
            roomPlayers.put(roomId, playersInRoom);

            Map<String, String> charactersInRoom = playerCharacters.getOrDefault(roomId, new HashMap<>());
            charactersInRoom.put(chatMessage.getSender(), "/image/pinkbin.png");
            playerCharacters.put(roomId, charactersInRoom);
        }

        Map<String, String> charactersInRoom = playerCharacters.get(roomId);
        StringBuilder allPlayersInfo = new StringBuilder();
        for (String player : playersInRoom) {
            allPlayersInfo.append(player).append(":").append(charactersInRoom.get(player)).append(",");
        }

        if (allPlayersInfo.length() > 0) {
            allPlayersInfo.deleteCharAt(allPlayersInfo.length() - 1); // 마지막 콤마 제거
        }

        ChatMessage newUserMessage = ChatMessage.builder()
                .type(ChatMessage.MessageType.JOIN)
                .content(allPlayersInfo.toString())
                .roomId(roomId)
                .build();

        messagingTemplate.convertAndSend("/topic/public", newUserMessage);

        System.out.println("room number: " + newUserMessage.getRoomId());
        System.out.println("Sending existing players: " + newUserMessage.getContent());

        return newUserMessage;
    }


    // 준비완료
    @MessageMapping("/chat.ready")
    @SendTo("/topic/public")
    public ChatMessage readyUser(ChatMessage chatMessage) {

        System.out.println(chatMessage.getType());
        System.out.println(chatMessage);
        return chatMessage;
    }

    //캐릭터 고를때
    @MessageMapping("/chat.selectCharacter")
    public void selectCharacter(ChatMessage chatMessage) {
        String roomId = chatMessage.getRoomId();
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

                messagingTemplate.convertAndSend("/topic/public", errorMessage);
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
            messagingTemplate.convertAndSend("/topic/public", characterSelectMessage);
        }
    }


    @MessageMapping("/chat.deselectCharacter")
    public void deselectCharacter(ChatMessage chatMessage) {
        String roomId = chatMessage.getRoomId();
        String sender = chatMessage.getSender();

        if (playerCharacters.containsKey(roomId)) {
            Map<String, String> charactersInRoom = playerCharacters.get(roomId);
            charactersInRoom.remove(sender);
            playerCharacters.put(roomId, charactersInRoom);

            ChatMessage characterDeselectMessage = ChatMessage.builder()
                    .type(ChatMessage.MessageType.DESELECT)
                    .sender(sender)
                    .roomId(roomId)
                    .build();

            System.out.println("dsel: " + characterDeselectMessage);
            messagingTemplate.convertAndSend("/topic/public", characterDeselectMessage);
        }
    }




    // 유저 방나갈때
    @MessageMapping("/chat.leaveUser")
    @SendTo("/topic/public")
    public ChatMessage leaveUser(ChatMessage chatMessage) {
        String roomId = chatMessage.getRoomId();
        String sender = chatMessage.getSender();
        List<String> playersInRoom = roomPlayers.getOrDefault(roomId, new ArrayList<>());

        if (playersInRoom.contains(sender)) {
            playersInRoom.remove(sender);
            roomPlayers.put(roomId, playersInRoom);
        }

        // 플레이어의 캐릭터 이미지 제거
        if (playerCharacters.containsKey(roomId)) {
            Map<String, String> charactersInRoom = playerCharacters.get(roomId);
            charactersInRoom.remove(sender);
            playerCharacters.put(roomId, charactersInRoom);
        }

        ChatMessage leaveMessage = ChatMessage.builder()
                .type(ChatMessage.MessageType.LEAVE)
                .sender(sender)
                .content(String.join(",", playersInRoom))
                .roomId(roomId)
                .build();

        System.out.println(leaveMessage);
        return leaveMessage;
    }


    //채팅방
    @MessageMapping("/chat.sendMessage")
    @SendTo("/topic/public")
    public ChatMessage sendMessage(ChatMessage chatMessage) {
        System.out.println("chatMessage: " + chatMessage);
        return chatMessage;
    }







}
