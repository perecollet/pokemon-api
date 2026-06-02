package com.alea.pokemon.infrastructure.adapter.out.pokeapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PokemonDetailResponse(
        Integer id,
        String name,
        @JsonProperty("base_experience") Integer baseExperience,
        int height,
        int weight
) {}