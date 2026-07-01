package com.alea.pokemon.domain.model;

import lombok.SneakyThrows;

/**
 * Domain model for a Pokémon.
 *
 * Units follow PokéAPI conventions:
 * - weight in hectograms (1 hg = 100 g)
 * - height in decimeters (1 dm = 10 cm)
 *
 * Unit conversion happens at the API boundary (response DTO).
 */
public record Pokemon (
        Integer id,
        String name,
        Integer baseExperience,
        int height,
        int weight
){
    public Pokemon {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("id must be a positive integer");
        }

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }

        name = name.strip();

        if (height < 0) {
            throw new IllegalArgumentException("height must not be negative");
        }

        if (weight < 0) {
            throw new IllegalArgumentException("weight must not be negative");
        }
    }
}
