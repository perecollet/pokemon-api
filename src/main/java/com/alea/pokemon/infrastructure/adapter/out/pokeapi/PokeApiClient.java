package com.alea.pokemon.infrastructure.adapter.out.pokeapi;

import com.alea.pokemon.domain.model.Pokemon;
import com.alea.pokemon.domain.port.out.PokemonCatalogProvider;
import com.alea.pokemon.infrastructure.adapter.out.pokeapi.dto.PokemonDetailResponse;
import com.alea.pokemon.infrastructure.adapter.out.pokeapi.dto.PokemonListResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class PokeApiClient implements PokemonCatalogProvider {

    private static final Logger log = LoggerFactory.getLogger(PokeApiClient.class);
    private static final int CONCURRENCY = 20;
    private static final int LIST_LIMIT = 2000;

    private final RestClient restClient;

    public PokeApiClient(RestClient pokeApiRestClient) {
        this.restClient = pokeApiRestClient;
    }

    @Override
    public List<Pokemon> fetchAll() {
        PokemonListResponse list = fetchList();
        return fetchDetailsInParallel(list.results());
    }

    private PokemonListResponse fetchList() {
        return restClient.get()
                .uri(uri -> uri.path("/pokemon").queryParam("limit", LIST_LIMIT).build())
                .retrieve()
                .body(PokemonListResponse.class);
    }

    private List<Pokemon> fetchDetailsInParallel(List<PokemonListResponse.Result> results) {
        try (ExecutorService executor = Executors.newFixedThreadPool(CONCURRENCY)) {
            List<CompletableFuture<Pokemon>> futures = results.stream()
                    .map(r -> CompletableFuture.supplyAsync(() -> fetchDetail(r.url()), executor))
                    .toList();

            return futures.stream()
                    .map(CompletableFuture::join)
                    .filter(Objects::nonNull)
                    .toList();
        }
    }

    private Pokemon fetchDetail(String url) {
        try {
            PokemonDetailResponse d = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(PokemonDetailResponse.class);
            return new Pokemon(d.id(), d.name(), d.baseExperience(), d.height(), d.weight());
        } catch (Exception e) {
            log.warn("Failed to fetch Pokemon detail at {}: {}", url, e.getMessage());
            return null;
        }
    }
}