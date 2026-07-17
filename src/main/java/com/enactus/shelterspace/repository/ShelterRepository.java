package com.enactus.shelterspace.repository;

import com.enactus.shelterspace.model.Shelter;
import com.enactus.shelterspace.model.enums.ShelterStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShelterRepository extends JpaRepository<Shelter, Long> {

    List<Shelter> findByOperationalStatus(ShelterStatus operationalStatus);
}
