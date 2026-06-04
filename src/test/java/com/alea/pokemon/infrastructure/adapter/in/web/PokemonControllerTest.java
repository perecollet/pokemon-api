package com.alea.pokemon.infrastructure.adapter.in.web;

import com.alea.pokemon.domain.exception.PokemonDataNotReadyException;
import com.alea.pokemon.domain.model.Pokemon;
import com.alea.pokemon.domain.port.in.GetTopPokemonUseCase;
import com.alea.pokemon.infrastructure.adapter.in.web.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PokemonController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("PokemonController")
class PokemonControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GetTopPokemonUseCase useCase;

    private final Pokemon pikachu = new Pokemon(25, "pikachu", 112, 4, 60);
    private final Pokemon charizard = new Pokemon(6, "charizard", 267, 17, 905);

    @Test
    @DisplayName("GET /heaviest returns top pokemons with units converted")
    void getHeaviest() throws Exception {
        when(useCase.byWeight(5)).thenReturn(List.of(charizard, pikachu));

        mockMvc.perform(get("/api/v1/pokemon/top/heaviest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("charizard"))
                .andExpect(jsonPath("$[0].weightKg").value(90.5))
                .andExpect(jsonPath("$[0].heightCm").value(170))
                .andExpect(jsonPath("$[1].weightKg").value(6.0))
                .andExpect(jsonPath("$[1].heightCm").value(40));
    }

    @Test
    @DisplayName("GET /tallest returns top pokemons by height")
    void getTallest() throws Exception {
        when(useCase.byHeight(5)).thenReturn(List.of(charizard));

        mockMvc.perform(get("/api/v1/pokemon/top/tallest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("charizard"));
    }

    @Test
    @DisplayName("GET /most-experienced returns top pokemons by base experience")
    void getMostExperienced() throws Exception {
        when(useCase.byBaseExperience(5)).thenReturn(List.of(charizard));

        mockMvc.perform(get("/api/v1/pokemon/top/most-experienced"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].baseExperience").value(267));
    }

    @Test
    @DisplayName("returns 503 when catalog is not ready")
    void returns503WhenNotReady() throws Exception {
        when(useCase.byWeight(5)).thenThrow(new PokemonDataNotReadyException());

        mockMvc.perform(get("/api/v1/pokemon/top/heaviest"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.title").value("Catalog not ready"));
    }

    @Test
    @DisplayName("returns 400 when limit is out of range")
    void returns400OnInvalidLimit() throws Exception {
        mockMvc.perform(get("/api/v1/pokemon/top/heaviest").param("limit", "999"))
                .andExpect(status().isBadRequest());
    }
}