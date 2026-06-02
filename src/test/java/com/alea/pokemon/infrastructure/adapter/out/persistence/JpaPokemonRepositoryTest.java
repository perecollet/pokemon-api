package com.alea.pokemon.infrastructure.adapter.out.persistence;

import com.alea.pokemon.domain.model.Pokemon;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@DisplayName("JpaPokemonRepository (integration)")
class JpaPokemonRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("pokemon")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private JpaPokemonRepository repository;

    @Autowired
    private PokemonJpaRepository jpaRepository;

    private final Pokemon pikachu = new Pokemon(25, "pikachu", 112, 4, 60);
    private final Pokemon charizard = new Pokemon(6, "charizard", 267, 17, 905);
    private final Pokemon snorlax = new Pokemon(143, "snorlax", 189, 21, 4600);
    private final Pokemon onix = new Pokemon(95, "onix", 77, 88, 2100);
    private final Pokemon caterpie = new Pokemon(10, "caterpie", 39, 3, 29);
    private final Pokemon legendaryNoExp = new Pokemon(999, "mystery", null, 10, 100);

    @BeforeEach
    void cleanDatabase() {
        jpaRepository.deleteAll();
    }

    @Nested
    @DisplayName("saveAll")
    class SaveAll {

        @Test
        @DisplayName("persists pokemons to the database")
        void persistsPokemons() {
            repository.saveAll(List.of(pikachu, charizard));

            assertThat(jpaRepository.count()).isEqualTo(2);
        }

        @Test
        @DisplayName("upserts by id (no duplicates on repeated sync)")
        void upsertsById() {
            repository.saveAll(List.of(pikachu));
            repository.saveAll(List.of(pikachu));

            assertThat(jpaRepository.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("handles null base experience")
        void handlesNullBaseExperience() {
            repository.saveAll(List.of(legendaryNoExp));

            PokemonEntity stored = jpaRepository.findById(999).orElseThrow();
            assertThat(stored.getBaseExperience()).isNull();
        }
    }

    @Nested
    @DisplayName("findTopByWeight")
    class FindTopByWeight {

        @Test
        @DisplayName("returns top 5 heaviest in descending order")
        void returnsTop5HeaviestDesc() {
            repository.saveAll(List.of(pikachu, charizard, snorlax, onix, caterpie));

            List<Pokemon> result = repository.findTopByWeight(5);

            assertThat(result)
                    .extracting(Pokemon::name)
                    .containsExactly("snorlax", "onix", "charizard", "pikachu", "caterpie");
        }

        @Test
        @DisplayName("returns empty list when no pokemons stored")
        void returnsEmptyWhenNoData() {
            List<Pokemon> result = repository.findTopByWeight(5);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findTopByHeight")
    class FindTopByHeight {

        @Test
        @DisplayName("returns top 5 tallest in descending order")
        void returnsTop5TallestDesc() {
            repository.saveAll(List.of(pikachu, charizard, snorlax, onix, caterpie));

            List<Pokemon> result = repository.findTopByHeight(5);

            assertThat(result)
                    .extracting(Pokemon::name)
                    .containsExactly("onix", "snorlax", "charizard", "pikachu", "caterpie");
        }
    }

    @Nested
    @DisplayName("findTopByBaseExperience")
    class FindTopByBaseExperience {

        @Test
        @DisplayName("returns top 5 by base experience in descending order")
        void returnsTop5ByBaseExperienceDesc() {
            repository.saveAll(List.of(pikachu, charizard, snorlax, onix, caterpie));

            List<Pokemon> result = repository.findTopByBaseExperience(5);

            assertThat(result)
                    .extracting(Pokemon::name)
                    .containsExactly("charizard", "snorlax", "pikachu", "onix", "caterpie");
        }

        @Test
        @DisplayName("nulls in base experience are ranked last")
        void nullsRankedLast() {
            repository.saveAll(List.of(pikachu, legendaryNoExp));

            List<Pokemon> result = repository.findTopByBaseExperience(5);

            assertThat(result)
                    .extracting(Pokemon::name)
                    .containsExactly("pikachu", "mystery");
        }
    }

    @Nested
    @DisplayName("isEmpty")
    class IsEmpty {

        @Test
        @DisplayName("returns true when database is empty")
        void emptyByDefault() {
            assertThat(repository.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("returns false after saving pokemons")
        void notEmptyAfterSave() {
            repository.saveAll(List.of(pikachu));

            assertThat(repository.isEmpty()).isFalse();
        }
    }
}