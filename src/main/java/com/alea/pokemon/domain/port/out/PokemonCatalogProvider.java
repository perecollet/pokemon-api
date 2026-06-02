package com.alea.pokemon.domain.port.out;

import com.alea.pokemon.domain.model.Pokemon;

import java.util.List;

public interface PokemonCatalogProvider {

    List<Pokemon> fetchAll();
}