package org.example.websockettest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class ChattingDto {

        private Long roomId;           // 채팅방 ID
        private Long senderId;         // 보낸 사람 ID
        private Long receiverId;       // 받는 사람 ID
        private Long friendRelationId; // 친구 관계 ID
        private String messageContent; // 메시지 내용

}
