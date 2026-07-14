package com.enactus.shelterspace.service;

import com.enactus.shelterspace.dto.ShelterRequest;
import com.enactus.shelterspace.model.Shelter;
import com.enactus.shelterspace.repository.ShelterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Business logic lives here. The controller calls these methods; these
 * methods call the repository.
 *
 * @RequiredArgsConstructor (Lombok) generates a constructor for all 'final'
 * fields, so Spring injects the repository automatically. This is
 * constructor injection — preferred over @Autowired on a field.
 */
@Service
@RequiredArgsConstructor
public class ShelterService {

    private final ShelterRepository shelterRepository;

    /**
     * Return every shelter.
     * TODO: which repository method gives you all rows?
     */
    public List<Shelter> getAll() {
        // TODO: implement
        return null;
    }

    /**
     * Find one shelter by id.
     * TODO: findById returns an Optional. What should happen if it's empty?
     *       (Hint: throw your ResourceNotFoundException.)
     */
    public Shelter getById(Long id) {
        // TODO: implement
        return null;
    }

    /**
     * Create a new shelter from the request DTO.
     * TODO:
     *   1. Make a new Shelter object.
     *   2. Copy fields from 'request' into it.
     *   3. Set any server-controlled defaults (e.g. starting occupancy).
     *   4. Save it and return the saved entity.
     */
    public Shelter create(ShelterRequest request) {
        // TODO: implement
        return null;
    }

    /**
     * Update an existing shelter.
     * TODO:
     *   1. Fetch the existing one (reuse getById — why is that handy?).
     *   2. Overwrite its fields from the request.
     *   3. Save and return.
     */
    public Shelter update(Long id, ShelterRequest request) {
        // TODO: implement
        return null;
    }

    /**
     * Delete a shelter by id.
     * TODO: should you check it exists first? What happens if it doesn't?
     */
    public void delete(Long id) {
        // TODO: implement
    }
}
