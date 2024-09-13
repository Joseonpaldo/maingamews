package org.example.websockettest.controller;

import com.google.gson.Gson;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.websockettest.dto.*;
import org.example.websockettest.entity.GameDataEntity;
import org.example.websockettest.entity.GameLogEntity;
import org.example.websockettest.entity.GameRoomEntity;
import org.example.websockettest.entity.UserEntity;
import org.example.websockettest.repository.GameDataRepositoryImpl;
import org.example.websockettest.repository.GameLogRepositoryImpl;
import org.example.websockettest.repository.GameRoomRepositoryImpl;
import org.example.websockettest.repository.UserRepositoryImpl;
import org.json.simple.JSONObject;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.sql.Timestamp;
import java.util.*;

@Controller
@RequiredArgsConstructor
public class MainGameController {
    public static Map<String, List<Integer>> yutResult = new HashMap<>();
    final private GameDataRepositoryImpl gameDataRepository;
    private final GameLogRepositoryImpl gameLogRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepositoryImpl userRepositoryImpl;
    private final GameRoomRepositoryImpl gameRoomRepositoryImpl;


    public Map<String, List<Player>> players = new HashMap<>();
    public Map<String, List<MainGameChatLogDto>> chatLogs = new HashMap<>();
    public Map<String, List<MainGameLoggingDto>> gameLogs = new HashMap<>();
    public Map<String, Integer> currentOrder = new HashMap<>();
    public Map<String, Boolean> currentThrow = new HashMap<>();
    public Map<String, String> sessionRoomId = new HashMap<>();
    public Map<String, Integer> bankruptcy = new HashMap<>();

    Gson gson = new Gson();


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
        if (roomId == null || roomId.isEmpty()) {
            return;
        }
        System.out.println("끊김 roomId: " + roomId + " sessionId: " + sessionId);
        var getPlayerData = players.get(roomId);
        for (Player player : getPlayerData) {
            if (player.getSessionId() != null && player.getSessionId().equals(sessionId)) {
                player.setSessionId("");
            }
        }

        sessionRoomId.remove(sessionId);
        sendPlayerInfo(roomId);
    }

    @MessageMapping("/main/start/{roomId}")
    public void mainGameStartHandle(@DestinationVariable String roomId) {
        newPlayerDataPush(roomId);
    }

    @MessageMapping("/main/join/{roomId}")
    public void handleGameMessage(@DestinationVariable String roomId, @Header String name, @Header String sessionId) {
        System.out.println("join " + name + " " + sessionId + " " + roomId);
        sessionRoomId.put(sessionId, roomId);
        var getPlayers = players.get(roomId);
        if (getPlayers == null) {
            System.out.println("join > roomId is null");
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
        for (Player player : players.get(roomId)) {
            if (player.isMyTurn() && player.getPlayer().equals(name)) {
                player.setLocation(location);

                SendMessage results = SendMessage.builder().Type("resultArr").Message(yutResult.get(roomId).toString()).build();
                messagingTemplate.convertAndSend("/topic/main-game/" + roomId, results);

                sendPlayerInfo(roomId);
            }
        }
    }

    @MessageMapping("/main/pay-toll/{roomId}")
    public void payToll(@DestinationVariable String roomId,
                        @Header String name,
                        @Header Integer location,
                        @Header Integer price,
                        @Header String owner) {
        int landmark = 0;
        //랜드마크 등급 찾기
        for (Player player : players.get(roomId)) {
            if (player.getPlayer().equals(owner)) {
                for (Location thisLocation : player.getEstate()) {
                    if (thisLocation.getLocation() == location) {
                        landmark = thisLocation.getLandmark();
                    }
                }
            }
        }

        //돈 뺐고 돈 넣기
        for (Player player : players.get(roomId)) {
            if (player.getPlayer().equals(name)) {
                int myMoney = (int) (player.getMoney() - (price * landmark * 100));
                player.setMoney(myMoney);
            }
            if (player.getPlayer().equals(owner)) {
                int myMoney = (int) (player.getMoney() + (price * landmark * 100));
                player.setMoney(myMoney);
            }
        }

        sendPlayerInfo(roomId);
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
        List<Player> playerList = players.get(roomId);

        // 자기 턴이 아닐 경우 요청을 무시
        if (playerList.stream().noneMatch(player -> player.isMyTurn() && player.getPlayer().equals(name))) {
            return;
        }

        int nextTurn = currentOrder.get(roomId) + 1;

        // 다음 턴 계산
        nextTurn = (nextTurn > 4) ? 1 : nextTurn;
        System.out.println("현재 턴 : " + nextTurn);

        // 죽은 플레이어 확인 및 턴 조정
        while (true) {
            boolean playerFound = false;
            for (Player player : playerList) {
                if (player.getMoney() < 0 && player.getOrder() == nextTurn) {
                    System.out.println("죽은 사람 : " + player.getPlayer());
                    nextTurn = (nextTurn % 4) + 1; // 다음 턴으로 이동
                    System.out.println("죽은사람 넘기고 : " + nextTurn);
                    playerFound = true;
                    break; // 한 번 죽은 플레이어를 찾으면 루프를 종료하고 다시 턴 계산
                }
            }
            if (!playerFound) {
                break; // 더 이상 죽은 플레이어가 없다면 루프 종료
            }
        }

        // 턴 업데이트
        currentOrder.put(roomId, nextTurn);
        currentThrow.put(roomId, true);

        // 플레이어 턴 설정
        for (Player player : playerList) {
            player.setMyTurn(player.getOrder() == nextTurn);
        }

        //순위 재 설정
        // money가 0보다 큰 플레이어만 필터링하고 정렬
        List<Player> sortedPlayers = playerList.stream()
                .filter(player -> player.getMoney() > 0)
                .sorted(Comparator.comparingDouble(Player::getMoney).reversed()) // money 기준으로 내림차순 정렬
                .toList();

            System.out.println("sortedPlayers");
        sortedPlayers.forEach(player ->{
            System.out.println(player.getName());
            System.out.println(player.getRank());
        });

        // 랭크 업데이트
        for (int i = 0; i < sortedPlayers.size(); i++) {
            Player player = sortedPlayers.get(i);
            player.setRank(i + 1); // 랭크는 1부터 시작
        }

        // 랭크가 업데이트된 플레이어를 playerList에 반영
        for (Player player : playerList) {
                sortedPlayers.stream()
                        .filter(sortedPlayer -> sortedPlayer.getPlayer().equals(player.getPlayer()))
                        .findFirst()
                        .ifPresent(sortedPlayer -> player.setRank(sortedPlayer.getRank())); // 순위가 있을 경우만 업데이트
        }

            System.out.println("playerList");
        playerList.forEach(player ->{
            System.out.println(player.getName());
            System.out.println(player.getRank());
        });


        // 변경된 playerList를 다시 players에 저장
        players.put(roomId, playerList);

        sendPlayerInfo(roomId);
    }


    @MessageMapping("/main/dead/{roomId}")
    public void userDead(@DestinationVariable String roomId, @Header String name) {
        List<Player> playerList = players.get(roomId);
        int bankruptcyCount = bankruptcy.getOrDefault(roomId, 4);
        System.out.println(bankruptcyCount);

        playerList.stream()
                .filter(player -> player.getPlayer().equals(name) && player.getMoney() < 0 && player.isLive())
                .findFirst()
                .ifPresent(player -> {
                    player.setRank(bankruptcyCount);
                    player.setEstate(null);
                    player.setLive(false);
                    sendPlayerInfo(roomId);

                    bankruptcy.put(roomId, bankruptcyCount - 1);

                    players.put(roomId, playerList);

                    SendMessage userDead = SendMessage.builder().Type("userDead").Message(name).build();
                    messagingTemplate.convertAndSend("/topic/main-game/" + roomId, userDead);

                    sendPlayerInfo(roomId);
                });
    }


    @MessageMapping("main/main-game/end/{roomId}")
    public void mainGameTheEnd(@DestinationVariable String roomId) {
        List<Player> playerList = players.get(roomId);

        long bankruptcyCount = playerList.stream()
                .filter(player -> player.getMoney() < 0)
                .count();

        if (bankruptcyCount == 3) {
            SendMessage theEnd = SendMessage.builder()
                    .Type("TheEnd")
                    .Message("the end")
                    .build();
            messagingTemplate.convertAndSend("/topic/main-game/" + roomId, theEnd);
        }
    }


    @MessageMapping("/main/chatLog/join/{roomId}")
    public void joinLog(@DestinationVariable String roomId) {
        System.out.println("join ");
        List<MainGameChatLogDto> chatLogList = chatLogs.get(roomId);
        if (chatLogList != null) {
            SendMessage chatLogMessage = SendMessage.builder().Type("chatLog").Message(gson.toJson(chatLogList)).build();
            messagingTemplate.convertAndSend("/topic/main-game/log/" + roomId, chatLogMessage);
        }

        List<MainGameLoggingDto> gameLogList = gameLogs.get(roomId);
        if (gameLogList == null) {
            gameLogList = new ArrayList<>();
        }
        MainGameLoggingDto joinLog = MainGameLoggingDto.builder().timestamp(new Timestamp(new Date().getTime())).message("입장").build();
        gameLogList.add(joinLog);
        gameLogs.put(String.valueOf(roomId), gameLogList);

        SendMessage gameLogMessage = SendMessage.builder().Type("gameLog").Message(gson.toJson(gameLogList)).build();
        messagingTemplate.convertAndSend("/topic/main-game/log/" + roomId, gameLogMessage);
    }

    @MessageMapping("/main/chatLog/{roomId}")
    public void chatLog(@DestinationVariable String roomId, @Header String name, @Payload String message) {
        MainGameChatLogDto chatLogDto = new MainGameChatLogDto();

        // 스트림을 사용하여 플레이어 찾기
        Optional<Player> optionalPlayer = players.get(roomId).stream()
                .filter(player -> player.getPlayer().equals(name))
                .findFirst();
        optionalPlayer.ifPresent(chatLogDto::setPlayer); // 플레이어가 존재하면 설정

        System.out.println(message);
        chatLogDto.setMessage(message);
        chatLogDto.setTimestamp(new Timestamp(new Date().getTime()));

        List<MainGameChatLogDto> chatLogList = chatLogs.get(roomId);
        if (chatLogList == null) {
            chatLogList = new ArrayList<>();
        }
        chatLogList.add(chatLogDto);

        chatLogs.put(roomId, chatLogList);

        SendMessage chatLogMessage = SendMessage.builder().Type("chatLog").Message(gson.toJson(chatLogDto)).build();
        messagingTemplate.convertAndSend("/topic/main-game/log/" + roomId, chatLogMessage);
    }


    @MessageMapping("/main/roulette/{roomId}")
    public void rouletteStart(@DestinationVariable String roomId, @Header String name) {
        players.get(roomId).forEach(player -> {
            if (player.isMyTurn() && player.getPlayer().equals(name)) {
                int rand = (int) (Math.random() * 6 + 1);

                SendMessage roulette = SendMessage.builder().Type("rouletteStart").Message(String.valueOf(rand)).build();
                messagingTemplate.convertAndSend("/topic/roulette/" + roomId, roulette);
            }
        });
    }


    @MessageMapping("/main/mini-game-step/{roomId}")
    public void miniGameStep(@DestinationVariable String roomId, @Header String name) {
        players.get(roomId).forEach(player -> {
            if (player.isMyTurn() && player.getPlayer().equals(name)) {
                SendMessage miniGameStep = SendMessage.builder().Type("commend").Message("mini-game-step-open").build();
                messagingTemplate.convertAndSend("/topic/main-game/" + roomId, miniGameStep);
            }
        });
    }


    @MessageMapping("/mini-game/result/{roomId}")
    public void miniGameGetResultNum(@DestinationVariable String roomId,
                                     @Header String name,
                                     @Header String number) {
        List<Player> roomPlayers = players.get(roomId);

        // 해당 플레이어가 턴을 가지고 있는지 확인
        Optional<Player> currentPlayer = roomPlayers.stream()
                .filter(player -> player.isMyTurn() && player.getPlayer().equals(name))
                .findFirst();

        currentPlayer.ifPresent(player -> {
            SendMessage miniGameStep = SendMessage.builder()
                    .Type("commend")
                    .Message("mini-game-step-close")
                    .build();
            messagingTemplate.convertAndSend("/topic/main-game/" + roomId, miniGameStep);

            SendMessage miniGameResult = SendMessage.builder()
                    .Type("whatGame")
                    .Message(number)
                    .build();
            messagingTemplate.convertAndSend("/topic/mini-game/" + roomId, miniGameResult);
        });
    }

    @MessageMapping("/mini-game/isWin/{roomId}")
    public void miniGameIsWin(@DestinationVariable String roomId, @Header String name, @Header Boolean result) {
        players.get(roomId).forEach(player -> {
            if (player.isMyTurn() && player.getPlayer().equals(name)) {
                player.setMoney(player.getMoney() + 500);

                SendMessage miniGameIsWin = SendMessage.builder().Type("isWinResult").Message(result.toString()).build();
                messagingTemplate.convertAndSend("/topic/main-game/" + roomId, miniGameIsWin);


                SendMessage passTurn = SendMessage.builder().Type("passTurn").Message(name).build();
                messagingTemplate.convertAndSend("/topic/main-game/" + roomId, passTurn);
            }
        });
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
        var findPlayer = players.get(roomId);
        if (findPlayer != null) {
            return;
        }
        System.out.println("newPlayerDataPush room id : " + roomId);
        List<Player> playerList = new ArrayList<>();
        var data = gameDataRepository.findAllByRoomId(Long.valueOf(roomId));
        for (GameDataEntity gameData : data) {
            Player player = Player.builder()
                    .userId(gameData.getUser().getUserId())
                    .name(gameData.getUser().getNickname())
                    .profile(gameData.getUser().getProfilePicture())
                    .location(0)
                    .money(gameData.getGameRoom().getBudget())
                    .avatar(gameData.getAvatar())
                    .player("player" + gameData.getMyTurn())
                    .order(gameData.getMyTurn())
                    .myTurn(false)
                    .rank(gameData.getMyTurn())
                    .live(true)
                    .build();
            if (gameData.getMyTurn() == 1) {
                player.setMyTurn(true);
            }
            playerList.add(player);
        }
        players.put(roomId, playerList);
        currentOrder.put(roomId, 1);
        currentThrow.put(roomId, true);

        System.out.println(playerList);
    }

    public void mainGameLogging(Long roomId, String message) {
        MainGameLoggingDto mainGameLoggingDto = new MainGameLoggingDto();
        mainGameLoggingDto.setMessage(message);
        mainGameLoggingDto.setTimestamp(new Timestamp(new Date().getTime()));

        List<MainGameLoggingDto> gameLogList = gameLogs.get(roomId.toString());
        gameLogList.add(mainGameLoggingDto);
        gameLogs.put(String.valueOf(roomId), gameLogList);

        SendMessage gameLogMessage = SendMessage.builder().Type("gameLog").Message(mainGameLoggingDto.toString()).build();
        messagingTemplate.convertAndSend("/topic/main-game/log/" + roomId, gameLogMessage);
    }


    @Transactional
    public void saveGameLog(Long userId, Long roomId, int type, String message) {
        UserEntity user = userRepositoryImpl.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        GameRoomEntity gameRoom = gameRoomRepositoryImpl.findById(roomId).orElseThrow(() -> new RuntimeException("Room not found"));

        GameLogEntity gameLog = GameLogEntity.builder()
                .user(user)
                .gameRoom(gameRoom)
                .type(type)
                .message(message)
                .build();

        gameLogRepository.save(gameLog);
    }


    @Transactional
    public void mainGameEnd(Long roomId, Long winUserId) {
        List<GameDataEntity> data = gameDataRepository.findAllByRoomId(roomId);

        // 사용자 데이터 업데이트
        Map<Long, UserEntity> userDataMap = new HashMap<>();
        for (GameDataEntity gameData : data) {
            var userData = gameData.getUser();
            userData.setTot4p(userData.getTot4p() + 1);
            if (userData.getUserId().equals(winUserId)) {
                userData.setWin4p(userData.getWin4p() + 1);
            }
            userDataMap.put(userData.getUserId(), userData);
        }

        // 사용자 데이터 일괄 저장
        userRepositoryImpl.saveAll(userDataMap.values());

        // 게임 방 상태 업데이트
        GameRoomEntity gameRoom = gameRoomRepositoryImpl.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));
        gameRoom.setRoomStatus(3);

        // 방 관련 데이터 제거
        removeRoomData(roomId);
    }

    private void removeRoomData(Long roomId) {
        players.remove(roomId.toString());
        chatLogs.remove(roomId.toString());
        gameLogs.remove(roomId.toString());
        currentOrder.remove(roomId.toString());
        currentThrow.remove(roomId.toString());
    }


}


