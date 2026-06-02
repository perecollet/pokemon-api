package com.alea.pokemon.infrastructure.adapter.out.persistence;

import com.alea.pokemon.domain.model.Pokemon;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class PokemonEntityMapper {

    public Pokemon toDomain(PokemonEntity entity) {
        return new Pokemon(
                entity.getId(),
                entity.getName(),
                entity.getBaseExperience(),
                entity.getHeight(),
                entity.getWeight()
        );
    }

    public PokemonEntity toEntity(Pokemon pokemon) {
        return new PokemonEntity(
                pokemon.id(),
                pokemon.name(),
                pokemon.baseExperience(),
                pokemon.height(),
                pokemon.weight(),
                Instant.now()
        );
    }
}