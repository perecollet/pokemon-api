package com.alea.pokemon.infrastructure.adapter.out.pokeapi;

import com.alea.pokemon.domain.model.Pokemon;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PokeApiClient")
class PokeApiClientTest {

    private static WireMockServer wireMock;
    private PokeApiClient client;

    @BeforeAll
    static void startWireMock() {
        wireMock = new WireMockServer(wireMockConfig().dynamicPort());
        wireMock.start();
    }

    @AfterAll
    static void stopWireMock() {
        wireMock.stop();
    }

    @BeforeEach
    void setup() {
        wireMock.resetAll();
        RestClient restClient = RestClient.builder()
                .baseUrl(wireMock.baseUrl())
                .build();
        client = new PokeApiClient(restClient);
    }

    @Test
    @DisplayName("fetches all pokemons combining list and detail endpoints")
    void fetchesAllPokemons() {
        wireMock.stubFor(get(urlPathEqualTo("/pokemon"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                    {
                      "count": 2,
                      "results": [
                        {"name": "bulbasaur", "url": "%s/pokemon/1/"},
                        {"name": "pikachu",   "url": "%s/pokemon/25/"}
                      ]
                    }
                    """.formatted(wireMock.baseUrl(), wireMock.baseUrl()))));

        wireMock.stubFor(get(urlPathEqualTo("/pokemon/1/"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                    {"id": 1, "name": "bulbasaur", "base_experience": 64, "height": 7, "weight": 69}
                    """)));

        wireMock.stubFor(get(urlPathEqualTo("/pokemon/25/"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                    {"id": 25, "name": "pikachu", "base_experience": 112, "height": 4, "weight": 60}
                    """)));

        List<Pokemon> result = client.fetchAll();

        assertThat(result)
                .extracting(Pokemon::name)
                .containsExactlyInAnyOrder("bulbasaur", "pikachu");
    }

    @Test
    @DisplayName("tolerates partial failures and returns successful fetches")
    void tolerantOfPartialFailures() {
        wireMock.stubFor(get(urlPathEqualTo("/pokemon"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                    {
                      "count": 2,
                      "results": [
                        {"name": "bulbasaur", "url": "%s/pokemon/1/"},
                        {"name": "broken",    "url": "%s/pokemon/999/"}
                      ]
                    }
                    """.formatted(wireMock.baseUrl(), wireMock.baseUrl()))));

        wireMock.stubFor(get(urlPathEqualTo("/pokemon/1/"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                    {"id": 1, "name": "bulbasaur", "base_experience": 64, "height": 7, "weight": 69}
                    """)));

        wireMock.stubFor(get(urlPathEqualTo("/pokemon/999/"))
                .willReturn(serverError()));

        List<Pokemon> result = client.fetchAll();

        assertThat(result)
                .extracting(Pokemon::name)
                .containsExactly("bulbasaur");
    }

    @Test
    @DisplayName("handles null base_experience in PokéAPI response")
    void handlesNullBaseExperience() {
        wireMock.stubFor(get(urlPathEqualTo("/pokemon"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                    {
                      "count": 1,
                      "results": [{"name": "mystery", "url": "%s/pokemon/999/"}]
                    }
                    """.formatted(wireMock.baseUrl()))));

        wireMock.stubFor(get(urlPathEqualTo("/pokemon/999/"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                    {"id": 999, "name": "mystery", "base_experience": null, "height": 10, "weight": 100}
                    """)));

        List<Pokemon> result = client.fetchAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).baseExperience()).isNull();
    }
}