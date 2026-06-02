package com.alea.pokemon.infrastructure.adapter.out.pokeapi;

import com.alea.pokemon.infrastructure.adapter.out.pokeapi.dto.PokemonDetailResponse;
import com.alea.pokemon.infrastructure.adapter.out.pokeapi.dto.PokemonListResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class PokeApiHttpClient {

    private static final int LIST_LIMIT = 2000;

    private final RestClient restClient;

    public PokeApiHttpClient(RestClient pokeApiRestClient) {
        this.restClient = pokeApiRestClient;
    }

    @Retry(name = "pokeapi")
    @CircuitBreaker(name = "pokeapi")
    public PokemonListResponse fetchList() {
        return restClient.get()
                .uri(uri -> uri.path("/pokemon").queryParam("limit", LIST_LIMIT).build())
                .retrieve()
                .body(PokemonListResponse.class);
    }

    @Retry(name = "pokeapi")
    @CircuitBreaker(name = "pokeapi")
    public PokemonDetailResponse fetchDetail(String url) {
        return restClient.get()
                .uri(url)
                .retrieve()
                .body(PokemonDetailResponse.class);
    }
}