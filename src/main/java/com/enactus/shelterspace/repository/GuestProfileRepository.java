package com.enactus.shelterspace.repository;

import com.enactus.shelterspace.model.GuestProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GuestProfileRepository extends JpaRepository<GuestProfile, Long> {
}
