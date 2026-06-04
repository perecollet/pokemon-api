package com.alea.pokemon.domain.port.out;

import com.alea.pokemon.domain.model.Pokemon;

import java.util.List;

public interface PokemonRepository {

    void saveAll(List<Pokemon> pokemon);

    List<Pokemon> findTopByWeight(int limit);

    List<Pokemon> findTopByHeight(int limit);

    List<Pokemon> findTopByBaseExperience(int limit);

    boolean isEmpty();

    long count();
}