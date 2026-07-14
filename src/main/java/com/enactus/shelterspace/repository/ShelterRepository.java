package com.enactus.shelterspace.repository;

import com.enactus.shelterspace.model.Shelter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Data-access layer for Shelter.
 *
 * By extending JpaRepository<Shelter, Long> you AUTOMATICALLY get:
 *   findAll(), findById(id), save(entity), deleteById(id), count(), etc.
 * You don't implement any of these — Spring generates them at runtime.
 *
 * TODO: Do you need any custom lookups? Spring Data derives queries from
 *       method names. Just declare the signature, no body needed.
 *
 *   Examples (uncomment / adapt if useful):
 *   List<Shelter> findByLocation(String location);
 *   List<Shelter> findByCapacityGreaterThan(int minCapacity);
 */
@Repository
public interface ShelterRepository extends JpaRepository<Shelter, Long> {

    // TODO: add custom query methods here (or leave empty for now)

}
