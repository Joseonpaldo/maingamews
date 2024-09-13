package org.example.websockettest.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class Player {
    private double userId;
    private String name;
    private String profile;
    private int location;
    private double money;
    private String avatar;
    private String player;
    private int order;
    private boolean myTurn;
    private List<Location> estate;
    private String SessionId;
    private int rank;
    private boolean live;
}
