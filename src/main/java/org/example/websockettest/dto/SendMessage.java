package org.example.websockettest.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SendMessage {
    private String Type;
    private String Message;
}
