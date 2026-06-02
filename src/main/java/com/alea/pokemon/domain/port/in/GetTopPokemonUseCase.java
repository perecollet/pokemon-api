package com.alea.pokemon.domain.port.in;

import com.alea.pokemon.domain.model.Pokemon;

import java.util.List;

public interface GetTopPokemonUseCase {

    List<Pokemon> byWeight(int limit);

    List<Pokemon> byHeight(int limit);

    List<Pokemon> byBaseExperience(int limit);
}