package com.alea.pokemon.infrastructure.adapter.in.web;

import com.alea.pokemon.domain.port.in.GetTopPokemonUseCase;
import com.alea.pokemon.infrastructure.adapter.in.web.dto.PokemonResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/pokemon/top")
@Validated
public class PokemonController {

    private final GetTopPokemonUseCase useCase;

    public PokemonController(GetTopPokemonUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping("/heaviest")
    public List<PokemonResponse> heaviest(@RequestParam(defaultValue = "5") @Min(1) @Max(50) int limit) {
        return useCase.byWeight(limit).stream().map(PokemonResponse::from).toList();
    }

    @GetMapping("/tallest")
    public List<PokemonResponse> tallest(@RequestParam(defaultValue = "5") @Min(1) @Max(50) int limit) {
        return useCase.byHeight(limit).stream().map(PokemonResponse::from).toList();
    }

    @GetMapping("/most-experienced")
    public List<PokemonResponse> mostExperienced(@RequestParam(defaultValue = "5") @Min(1) @Max(50) int limit) {
        return useCase.byBaseExperience(limit).stream().map(PokemonResponse::from).toList();
    }
}