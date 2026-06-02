package com.alea.pokemon.application.service;

import com.alea.pokemon.domain.exception.PokemonDataNotReadyException;
import com.alea.pokemon.domain.model.Pokemon;
import com.alea.pokemon.domain.port.in.GetTopPokemonUseCase;
import com.alea.pokemon.domain.port.out.PokemonRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PokemonRankingService implements GetTopPokemonUseCase {

    private final PokemonRepository repository;

    public PokemonRankingService(PokemonRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<Pokemon> byWeight(int limit) {
        ensureDataReady();
        return repository.findTopByWeight(limit);
    }

    @Override
    public List<Pokemon> byHeight(int limit) {
        ensureDataReady();
        return repository.findTopByHeight(limit);
    }

    @Override
    public List<Pokemon> byBaseExperience(int limit) {
        ensureDataReady();
        return repository.findTopByBaseExperience(limit);
    }

    private void ensureDataReady() {
        if (repository.isEmpty()) {
            throw new PokemonDataNotReadyException();
        }
    }
}