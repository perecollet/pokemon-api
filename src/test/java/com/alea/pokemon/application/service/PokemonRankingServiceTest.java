package com.alea.pokemon.application.service;

import com.alea.pokemon.domain.exception.PokemonDataNotReadyException;
import com.alea.pokemon.domain.model.Pokemon;
import com.alea.pokemon.domain.port.out.PokemonRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PokemonRankingService")
class PokemonRankingServiceTest {

    @Mock
    private RankingCacheReader cache;

    @Mock
    private PokemonRepository repository;

    @InjectMocks
    private PokemonRankingService service;

    private final Pokemon pikachu = new Pokemon(25, "pikachu", 112, 4, 60);
    private final Pokemon charizard = new Pokemon(6, "charizard", 267, 17, 905);
    private final Pokemon snorlax = new Pokemon(143, "snorlax", 189, 21, 4600);

    @Nested
    @DisplayName("byWeight")
    class ByWeight {

        @Test
        @DisplayName("returns cached heaviest Pokémon sliced by limit")
        void returnsCachedHeaviest() {
            when(repository.isEmpty()).thenReturn(false);
            when(cache.heaviest()).thenReturn(List.of(snorlax, charizard, pikachu));

            List<Pokemon> result = service.byWeight(5);

            assertThat(result).containsExactly(snorlax, charizard, pikachu);
        }

        @Test
        @DisplayName("throws when catalog is empty")
        void throwsWhenEmpty() {
            when(repository.isEmpty()).thenReturn(true);

            assertThatThrownBy(() -> service.byWeight(5))
                    .isInstanceOf(PokemonDataNotReadyException.class);
        }
    }

    @Nested
    @DisplayName("byHeight")
    class ByHeight {

        @Test
        @DisplayName("returns cached tallest Pokémon sliced by limit")
        void returnsCachedTallest() {
            when(repository.isEmpty()).thenReturn(false);
            when(cache.tallest()).thenReturn(List.of(snorlax, charizard, pikachu));

            List<Pokemon> result = service.byHeight(5);

            assertThat(result).containsExactly(snorlax, charizard, pikachu);
        }

        @Test
        @DisplayName("throws when catalog is empty")
        void throwsWhenEmpty() {
            when(repository.isEmpty()).thenReturn(true);

            assertThatThrownBy(() -> service.byHeight(5))
                    .isInstanceOf(PokemonDataNotReadyException.class);
        }
    }

    @Nested
    @DisplayName("byBaseExperience")
    class ByBaseExperience {

        @Test
        @DisplayName("returns cached most experienced Pokémon sliced by limit")
        void returnsCachedMostExperienced() {
            when(repository.isEmpty()).thenReturn(false);
            when(cache.mostExperienced()).thenReturn(List.of(charizard, snorlax, pikachu));

            List<Pokemon> result = service.byBaseExperience(5);

            assertThat(result).containsExactly(charizard, snorlax, pikachu);
        }

        @Test
        @DisplayName("throws when catalog is empty")
        void throwsWhenEmpty() {
            when(repository.isEmpty()).thenReturn(true);

            assertThatThrownBy(() -> service.byBaseExperience(5))
                    .isInstanceOf(PokemonDataNotReadyException.class);
        }
    }

    @Nested
    @DisplayName("limit handling")
    class LimitHandling {

        @Test
        @DisplayName("slices cached results to the requested limit")
        void slicesCachedResults() {
            when(repository.isEmpty()).thenReturn(false);
            when(cache.heaviest()).thenReturn(List.of(snorlax, charizard, pikachu));

            List<Pokemon> result = service.byWeight(2);

            assertThat(result).containsExactly(snorlax, charizard);
        }

        @Test
        @DisplayName("returns all cached results when limit exceeds cached size")
        void limitExceedsCachedSize() {
            when(repository.isEmpty()).thenReturn(false);
            when(cache.heaviest()).thenReturn(List.of(snorlax, charizard));

            List<Pokemon> result = service.byWeight(10);

            assertThat(result).containsExactly(snorlax, charizard);
        }
    }
}