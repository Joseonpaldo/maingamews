package org.example.websockettest.controller;

import lombok.RequiredArgsConstructor;
import org.example.websockettest.entity.FriendLogEntity;
import org.example.websockettest.repository.FriendLogRepositoryImpl;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/oneChat")
public class OnetoOneChatController {
    private final SimpMessagingTemplate messagingTemplate;
    private final FriendLogRepositoryImpl friendLogRepository;

    @MessageMapping("/chat.private.{recipient}")
    public void privateChat(@DestinationVariable Long recipient, @Payload FriendLogEntity friendLogEntity) {
        //FriendLogEntity json :  sender(나), receiver(친구), message, messageType 담아서 전송 후 이 곳에서 stamp 등록
        friendLogEntity.setMsgTime(Timestamp.valueOf(LocalDateTime.now()));
        friendLogRepository.save(friendLogEntity);

        messagingTemplate.convertAndSendToUser(recipient.toString(), "/user/queue/private", friendLogEntity);
    }

    @MessageMapping("/chat.history.{recipient}")
    public void privateHistory(@DestinationVariable Long recipient, @Payload FriendLogEntity request) {
        //내가 보낸 메세지 리스트
        List<FriendLogEntity> history = friendLogRepository.findBySenderIdAndRecipientId(request.getSenderId(), recipient);
        //상대방 -> 나에게 보낸 메세지 리스트에 저장
        history.addAll(friendLogRepository.findBySenderIdAndRecipientId(recipient, request.getSenderId()));

        history.sort(Comparator.comparing(FriendLogEntity::getMsgTime));

        for(FriendLogEntity f : history) {
            //f를 json으로 변경
            messagingTemplate.convertAndSendToUser(request.getSenderId().toString(), "/user/queue/private", f);
        }
    }

}
