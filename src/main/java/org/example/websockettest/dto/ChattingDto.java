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

        private Long senderId;
        private Long receiverId;
        private String messageContent;
        private String timestamp;

}
