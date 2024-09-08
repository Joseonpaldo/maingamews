package org.example.websockettest.repository;

import org.example.websockettest.entity.FriendRelationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FriendRelationRepositoryImpl extends JpaRepository<FriendRelationEntity, Long> {
    Long countByUserId1AndUserId2OrUserId2AndUserId1(Long userId1, Long userId2, Long userId3, Long userId4);

    // 둘 다 jwt my id값
    List<FriendRelationEntity> findByUserId1OrUserId2(Long userId1, Long userId2);

    void deleteByUserId1AndUserId2OrUserId2AndUserId1(Long userId1, Long userId2, Long userId3, Long userId4);

    Optional<FriendRelationEntity> findByUserId1AndUserId2OrUserId2AndUserId1(Long userId1, Long userId2, Long userId2Reversed, Long userId1Reversed);


}
