package org.example.websockettest.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.sql.Timestamp;

@Entity
@Table(name = "friend_log")
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class FriendLogEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "sender_id")
    private Long senderId;
    @Column(name = "recipient_id")
    private Long recipientId;
    @Column(name = "chat_log")
    private String chatLog;
    @Column(name = "msg_time")
    private Timestamp msgTime;
}
