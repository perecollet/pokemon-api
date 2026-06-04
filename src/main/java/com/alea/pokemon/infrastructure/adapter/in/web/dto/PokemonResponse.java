package com.alea.pokemon.infrastructure.adapter.in.web.dto;

import com.alea.pokemon.domain.model.Pokemon;

public record PokemonResponse(
        Integer id,
        String name,
        double weightKg,
        int heightCm,
        Integer baseExperience
) {

    public static PokemonResponse from(Pokemon p) {
        return new PokemonResponse(
                p.id(),
                p.name(),
                p.weight() / 10.0,
                p.height() * 10,
                p.baseExperience()
        );
    }
}