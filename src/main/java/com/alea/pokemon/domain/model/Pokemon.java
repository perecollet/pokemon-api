package com.alea.pokemon.domain.model;

public record Pokemon (
        Integer id,
        String name,
        int weight,
        int height,
        Integer baseExperience
){
    public Pokemon {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("id must be a positive integer");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (weight < 0) {
            throw new IllegalArgumentException("weight must not be negative");
        }
        if (height < 0) {
            throw new IllegalArgumentException("height must not be negative");
        }
    }
}
