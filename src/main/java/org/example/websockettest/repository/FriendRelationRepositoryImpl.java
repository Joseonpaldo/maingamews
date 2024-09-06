package org.example.websockettest.repository;

import org.example.websockettest.entity.FriendRelationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FriendRelationRepositoryImpl extends JpaRepository<FriendRelationEntity, Long> {
    Long countByUserId1OrUserId2AndUserId2OrUserId1(Long userId1, Long userId2, Long userId3, Long userId4);

    //둘다 jwt my id값
    List<FriendRelationEntity> findByUserId1OrUserId2(Long userId1, Long userId2);

    void deleteByUserId1OrUserId2AndUserId2OrUserId1(Long userId1, Long userId2, Long userId3, Long userId4);
}