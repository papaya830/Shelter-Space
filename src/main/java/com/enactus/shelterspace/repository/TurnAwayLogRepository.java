package com.enactus.shelterspace.repository;

import com.enactus.shelterspace.model.TurnAwayLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TurnAwayLogRepository extends JpaRepository<TurnAwayLog, Long> {
}
