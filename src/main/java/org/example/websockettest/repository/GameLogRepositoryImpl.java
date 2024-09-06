package org.example.websockettest.repository;

import org.example.websockettest.entity.GameLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameLogRepositoryImpl extends JpaRepository<GameLogEntity, Long> {

}
