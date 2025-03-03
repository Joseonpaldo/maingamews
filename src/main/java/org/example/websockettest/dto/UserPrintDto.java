package org.example.websockettest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPrintDto {
    private Long user_id;   //그냥 인덱스
    private String email;   //요놈이 아이디
    private String nickname;
    private String socialProvider;
    private String profilePicture;
    private int tot_2p;
    private int win_2p;
    private int tot_4p;
    private int win_4p;
}