package com.alea.pokemon.infrastructure.adapter.out.pokeapi;

import com.alea.pokemon.domain.model.Pokemon;
import com.alea.pokemon.domain.port.out.PokemonCatalogProvider;
import com.alea.pokemon.infrastructure.adapter.out.pokeapi.dto.PokemonDetailResponse;
import com.alea.pokemon.infrastructure.adapter.out.pokeapi.dto.PokemonListResponse;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class PokeApiClient implements PokemonCatalogProvider {

    private static final Logger log = LoggerFactory.getLogger(PokeApiClient.class);

    private final PokeApiHttpClient httpClient;
    private final int concurrency;

    public PokeApiClient(PokeApiHttpClient httpClient, @Value("${alea.pokeapi.fetch-concurrency:20}") int concurrency) {
        this.httpClient = httpClient;
        this.concurrency = concurrency;
    }
    @Override
    public List<Pokemon> fetchSince(long offset) {
        if (offset > Integer.MAX_VALUE) {
            throw new IllegalStateException("Offset exceeds int range: " + offset);
        }

        PokemonListResponse listing = httpClient.fetchList((int) offset, Integer.MAX_VALUE);
        int remoteTotal = listing.count();

        if (offset >= remoteTotal) {
            log.info("No new Pokemon to fetch (local={}, remote={})", offset, remoteTotal);
            return List.of();
        }

        log.info("Fetching {} new Pokemon (offset={}, remote total={})",
                listing.results().size(), offset, remoteTotal);

        return fetchDetailsInParallel(listing.results());
    }

    private List<Pokemon> fetchDetailsInParallel(List<PokemonListResponse.Result> results) {
        try (ExecutorService executor = Executors.newFixedThreadPool(concurrency)) {
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
        } catch (CallNotPermittedException e) {
            log.debug("Circuit breaker open, skipping {}", url);
            return null;
        } catch (Exception e) {
            log.warn("Failed to fetch Pokemon detail at {}: {}", url, e.getMessage());
            return null;
        }
    }
}