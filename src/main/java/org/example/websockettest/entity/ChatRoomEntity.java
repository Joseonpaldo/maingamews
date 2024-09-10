package org.example.websockettest.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "chat_room")
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class ChatRoomEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long roomId;

    @ManyToOne(fetch = FetchType.EAGER)  // 지연 로딩 대신 즉시 로딩으로 변경
    @JoinColumn(name = "friend_relation_id", nullable = false)
    private FriendRelationEntity friendRelation;

    @Column(nullable = false)
    private boolean isActive; // 방이 활성화 상태인지 여부
}
