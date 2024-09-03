package org.example.websockettest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MainGameChatLogDto {
    private Player player;
    private String message;
    private Timestamp timestamp;
}
