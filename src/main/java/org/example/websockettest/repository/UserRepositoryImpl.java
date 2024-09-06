package org.example.websockettest.repository;

import org.example.websockettest.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepositoryImpl extends JpaRepository<UserEntity, Long> {
    UserEntity findByuserId(Long user_id);

    UserEntity findByEmail(String email);

    long countByEmailAndSocialProvider(String email, String socialProvider);

    UserEntity findByUserIdentifyId(String identifyId);

}
