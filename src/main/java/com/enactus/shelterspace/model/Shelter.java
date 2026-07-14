package com.enactus.shelterspace.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The Shelter entity — maps to a database table.
 *
 * TODO: Decide what fields a shelter needs.
 *   - What identifies it? (already given: id)
 *   - What describes it? (name? location? capacity?)
 *   - What state does it track? (how full is it right now?)
 *
 * For each field, ask:
 *   - Should it be required?  -> @Column(nullable = false)
 *   - Is it a number, text, date?
 */
@Entity
@Table(name = "shelters")
@Data                 // Lombok: generates getters/setters/toString/equals
@NoArgsConstructor    // Lombok: no-arg constructor (JPA needs this)
@AllArgsConstructor   // Lombok: constructor with all fields
public class Shelter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // TODO: add your fields here.
    // Example of the annotation style you'll use:
    //
    // @Column(nullable = false)
    // private String name;

}
