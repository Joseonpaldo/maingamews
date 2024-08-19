package org.example.websockettest.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PlayerOrder {
    private String name;
    private int order;
    private boolean myTurn = false;
}
