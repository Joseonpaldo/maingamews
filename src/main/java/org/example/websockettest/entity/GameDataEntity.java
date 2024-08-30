package org.example.websockettest.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;


@Entity
@Table(name = "game_data")
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class GameDataEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long data_id;
    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "user_id")
    private UserEntity user;
    @ManyToOne
    @JoinColumn(name = "room_id", referencedColumnName = "room_id")
    private GameRoomEntity gameRoom;
    @ColumnDefault("0")
    private int user_location;
    // JSON으로 통신할 예정(즉 JSON을 스트링으로 받아올 예정)
    @Column(name = "user_estate", columnDefinition = "json")
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode user_estate;
    private int my_turn;
    private double money;
    private String avatar;
}
