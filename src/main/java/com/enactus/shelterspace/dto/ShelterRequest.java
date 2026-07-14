package com.enactus.shelterspace.dto;

import lombok.Data;

/**
 * ShelterRequest — the shape of data a CLIENT sends when creating/updating
 * a shelter. Deliberately NOT the same as the entity.
 *
 * Why separate from the Shelter entity?
 *   - You don't want clients setting 'id' or internal state directly.
 *   - You can validate input here without cluttering the entity.
 *
 * TODO: Add the fields a client should be allowed to send.
 *       Then add validation annotations to enforce rules, e.g.:
 *
 *   @NotBlank(message = "Name is required")
 *   private String name;
 *
 *   @Min(value = 0, message = "Capacity cannot be negative")
 *   private int capacity;
 *
 * (Validation annotations come from jakarta.validation.constraints.*)
 */
@Data
public class ShelterRequest {

    // TODO: add request fields + validation annotations

}
