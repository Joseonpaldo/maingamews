package org.example.websockettest.service;

import lombok.RequiredArgsConstructor;
import org.example.websockettest.entity.UserEntity;
import org.example.websockettest.repository.FriendRelationRepositoryImpl;
import org.example.websockettest.repository.UserRepositoryImpl;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepositoryImpl userRepositoryImpl;
    private final FriendRelationRepositoryImpl friendImpl;

    public UserEntity getUser(Long user_id) {
        UserEntity user;
        try {
            user = userRepositoryImpl.findById(user_id).get();
        } catch (Exception e) {
            return new UserEntity();
        }
        return user;
    }

    public UserEntity getUser(String userIdentifyId) {
        return userRepositoryImpl.findByUserIdentifyId(userIdentifyId);
    }

    public String getNicknameByUserIdentifyId(Long userId) {
        UserEntity user = getUser(userId);

        System.out.println(user);
        return user != null ? user.getNickname() : null;
    }

    public int getRankForUser(Long userId, Map<Long, Integer> playerRankMap) {
        // 플레이어 순위를 저장한 Map을 기반으로 순위를 반환하는 로직
        Integer rank = playerRankMap.get(userId);

        if (rank == null) {
            throw new IllegalArgumentException("플레이어가 없습니다.");
        }

        return rank;  // rank는 1, 2, 3, 4와 같은 정수로 반환됩니다.
    }


}
