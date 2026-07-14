package com.enactus.shelterspace.controller;

import com.enactus.shelterspace.dto.ShelterRequest;
import com.enactus.shelterspace.model.Shelter;
import com.enactus.shelterspace.service.ShelterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST layer. Maps HTTP requests to service calls.
 *
 * @RestController      = this class returns data (JSON), not view names.
 * @RequestMapping      = base path for every endpoint below.
 *
 * The annotations you'll need per method:
 *   @GetMapping, @PostMapping, @PutMapping, @DeleteMapping
 *   @PathVariable  -> pull {id} out of the URL
 *   @RequestBody   -> parse the JSON body into a ShelterRequest
 *   @Valid         -> trigger the validation annotations on the DTO
 */
@RestController
@RequestMapping("/api/shelters")
@RequiredArgsConstructor
public class ShelterController {

    private final ShelterService shelterService;

    /**
     * GET /api/shelters  -> list all
     * TODO: call the service and return the list.
     */
    @GetMapping
    public List<Shelter> getAll() {
        // TODO: implement
        return null;
    }

    /**
     * GET /api/shelters/{id}  -> one shelter
     * TODO: grab the id from the path, return the shelter.
     */
    @GetMapping("/{id}")
    public Shelter getById(/* TODO: @PathVariable */ Long id) {
        // TODO: implement
        return null;
    }

    /**
     * POST /api/shelters  -> create
     * TODO:
     *   - accept a validated request body
     *   - call the service
     *   - return 201 Created (look at ResponseEntity for setting status)
     */
    @PostMapping
    public ResponseEntity<Shelter> create(/* TODO: @Valid @RequestBody */ ShelterRequest request) {
        // TODO: implement
        return null;
    }

    /**
     * PUT /api/shelters/{id}  -> update
     * TODO: combine @PathVariable id + @Valid @RequestBody request.
     */
    @PutMapping("/{id}")
    public Shelter update(Long id, ShelterRequest request) {
        // TODO: implement
        return null;
    }

    /**
     * DELETE /api/shelters/{id}  -> delete
     * TODO: call the service, return 204 No Content.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(Long id) {
        // TODO: implement
        return null;
    }
}
