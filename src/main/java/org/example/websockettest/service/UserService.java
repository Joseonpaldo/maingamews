package org.example.websockettest.service;

import lombok.RequiredArgsConstructor;
import org.example.websockettest.entity.UserEntity;
import org.example.websockettest.repository.FriendRelationRepositoryImpl;
import org.example.websockettest.repository.UserRepositoryImpl;
import org.springframework.stereotype.Service;

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

}
