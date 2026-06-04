package com.alea.pokemon.infrastructure.adapter.out.pokeapi;

import com.alea.pokemon.infrastructure.adapter.out.pokeapi.dto.PokemonDetailResponse;
import com.alea.pokemon.infrastructure.adapter.out.pokeapi.dto.PokemonListResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Objects;

@Component
public class PokeApiHttpClient {

    private final RestClient restClient;

    public PokeApiHttpClient(RestClient pokeApiRestClient) {
        this.restClient = pokeApiRestClient;
    }

    @Retry(name = "pokeapi")
    @CircuitBreaker(name = "pokeapi")
    public PokemonListResponse fetchList(int offset, int limit) {
        PokemonListResponse response = restClient.get()
                .uri(uri -> uri.path("/pokemon")
                        .queryParam("offset", offset)
                        .queryParam("limit", limit)
                        .build())
                .retrieve()
                .body(PokemonListResponse.class);
        return Objects.requireNonNull(response, "PokéAPI returned empty list response");
    }

    @Retry(name = "pokeapi")
    @CircuitBreaker(name = "pokeapi")
    public PokemonDetailResponse fetchDetail(String url) {
        PokemonDetailResponse response = restClient.get()
                .uri(url)
                .retrieve()
                .body(PokemonDetailResponse.class);
        return Objects.requireNonNull(response, "PokéAPI returned empty detail response for " + url);
    }
}