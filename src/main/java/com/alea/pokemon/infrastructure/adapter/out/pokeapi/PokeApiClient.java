package com.alea.pokemon.infrastructure.adapter.out.pokeapi;

import com.alea.pokemon.domain.model.Pokemon;
import com.alea.pokemon.domain.port.out.PokemonCatalogProvider;
import com.alea.pokemon.infrastructure.adapter.out.pokeapi.dto.PokemonDetailResponse;
import com.alea.pokemon.infrastructure.adapter.out.pokeapi.dto.PokemonListResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class PokeApiClient implements PokemonCatalogProvider {

    private static final Logger log = LoggerFactory.getLogger(PokeApiClient.class);
    private static final int CONCURRENCY = 20;

    private final PokeApiHttpClient httpClient;

    public PokeApiClient(PokeApiHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public List<Pokemon> fetchSince(long offset) {
        PokemonListResponse first = httpClient.fetchList(0,1);
        int remoteTotal = first.count();

        if (offset >= remoteTotal) {
            log.info("No new Pokemon to fetch (local={}, remote={})", offset, remoteTotal);
            return List.of();
        }

        int newCount = (int) (remoteTotal - offset);
        log.info("Fetching {} new Pokemon (offset={}, remote total={})", newCount, offset, remoteTotal);

        PokemonListResponse listing = httpClient.fetchList((int) offset, newCount);
        return fetchDetailsInParallel(listing.results());
    }

    private List<Pokemon> fetchDetailsInParallel(List<PokemonListResponse.Result> results) {
        try (ExecutorService executor = Executors.newFixedThreadPool(CONCURRENCY)) {
            List<CompletableFuture<Pokemon>> futures = results.stream()
                    .map(r -> CompletableFuture.supplyAsync(() -> fetchDetailSafe(r.url()), executor))
                    .toList();

            return futures.stream()
                    .map(CompletableFuture::join)
                    .filter(Objects::nonNull)
                    .toList();
        }
    }

    private Pokemon fetchDetailSafe(String url) {
        try {
            PokemonDetailResponse d = httpClient.fetchDetail(url);
            return new Pokemon(d.id(), d.name(), d.baseExperience(), d.height(), d.weight());
        } catch (Exception e) {
            log.warn("Failed to fetch Pokemon detail at {}: {}", url, e.getMessage());
            return null;
        }
    }
}