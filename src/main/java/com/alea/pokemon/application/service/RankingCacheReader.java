package com.alea.pokemon.application.service;

import com.alea.pokemon.domain.model.Pokemon;
import com.alea.pokemon.domain.port.in.GetTopPokemonUseCase;
import com.alea.pokemon.domain.port.out.PokemonRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RankingCacheReader {

    private final PokemonRepository repository;

    public RankingCacheReader(PokemonRepository repository) {
        this.repository = repository;
    }

    @Cacheable(value = "topByWeight")
    public List<Pokemon> heaviest() {
        return repository.findTopByWeight(GetTopPokemonUseCase.MAX_LIMIT);
    }

    @Cacheable(value = "topByHeight")
    public List<Pokemon> tallest() {
        return repository.findTopByHeight(GetTopPokemonUseCase.MAX_LIMIT);
    }

    @Cacheable(value = "topByBaseExperience")
    public List<Pokemon> mostExperienced() {
        return repository.findTopByBaseExperience(GetTopPokemonUseCase.MAX_LIMIT);
    }
}