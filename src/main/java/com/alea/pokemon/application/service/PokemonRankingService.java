package com.alea.pokemon.application.service;

import com.alea.pokemon.domain.exception.PokemonDataNotReadyException;
import com.alea.pokemon.domain.model.Pokemon;
import com.alea.pokemon.domain.port.in.GetTopPokemonUseCase;
import com.alea.pokemon.domain.port.out.PokemonRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PokemonRankingService implements GetTopPokemonUseCase {

    private final RankingCacheReader cache;
    private final CatalogReadinessGate readinessGate;

    public PokemonRankingService(RankingCacheReader cache, CatalogReadinessGate readinessGate) {
        this.cache = cache;
        this.readinessGate = readinessGate;
    }

    @Override
    public List<Pokemon> byWeight(int limit) {
        ensureDataReady();
        return cache.heaviest().stream().limit(limit).toList();
    }

    @Override
    public List<Pokemon> byHeight(int limit) {
        ensureDataReady();
        return cache.tallest().stream().limit(limit).toList();
    }

    @Override
    public List<Pokemon> byBaseExperience(int limit) {
        ensureDataReady();
        return cache.mostExperienced().stream().limit(limit).toList();
    }

    private void ensureDataReady() {
        if (!readinessGate.awaitReady()) {
            throw new PokemonDataNotReadyException();
        }
    }
}