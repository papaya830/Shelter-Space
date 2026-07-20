package com.enactus.shelterspace.repository;

import com.enactus.shelterspace.model.GuestTypeInterest;
import com.enactus.shelterspace.model.enums.PopulationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GuestTypeInterestRepository extends JpaRepository<GuestTypeInterest, Long> {

    boolean existsByDeviceIdAndPopulationType(String deviceId, PopulationType populationType);

    @Query("SELECT g.populationType, COUNT(g) FROM GuestTypeInterest g GROUP BY g.populationType ORDER BY COUNT(g) DESC")
    List<Object[]> countByPopulationType();
}
