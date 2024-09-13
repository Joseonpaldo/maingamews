package org.example.websockettest.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PlayerResultDto {

    private String userid;
    private String rank;

}
