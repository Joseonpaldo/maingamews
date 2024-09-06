package org.example.websockettest.repository;

import org.example.websockettest.entity.FriendLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import java.util.List;


public interface FriendLogRepositoryImpl extends JpaRepository<FriendLogEntity, Long> {
    List<FriendLogEntity> findBySenderIdAndRecipientId(Long senderId, Long recipientId);
}
