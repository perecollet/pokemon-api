
package com.alea.pokemon.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Pokemon domain model")
class PokemonTest {

    @Nested
    @DisplayName("creation")
    class Creation {

        @Test
        @DisplayName("creates a valid Pokemon with all fields")
        void createsValidPokemon() {
            Pokemon pokemon = new Pokemon(25, "pikachu", 112, 4, 60);

            assertThat(pokemon.id()).isEqualTo(25);
            assertThat(pokemon.name()).isEqualTo("pikachu");
            assertThat(pokemon.baseExperience()).isEqualTo(112);
            assertThat(pokemon.height()).isEqualTo(4);
            assertThat(pokemon.weight()).isEqualTo(60);
        }

        @Test
        @DisplayName("allows null base experience")
        void allowsNullBaseExperience() {
            Pokemon pokemon = new Pokemon(1, "bulbasaur", null, 7, 69);

            assertThat(pokemon.baseExperience()).isNull();
        }

        @Test
        @DisplayName("allows zero weight and height")
        void allowsZeroWeightAndHeight() {
            Pokemon pokemon = new Pokemon(1, "ghost-form", 50, 0, 0);

            assertThat(pokemon.height()).isZero();
            assertThat(pokemon.weight()).isZero();
        }

        @Test
        @DisplayName("strips surrounding whitespace from name")
        void stripsName() {
            Pokemon pokemon = new Pokemon(25, "  pikachu  ", 112, 4, 60);

            assertThat(pokemon.name()).isEqualTo("pikachu");
        }
    }

    @Nested
    @DisplayName("validation")
    class Validation {

        @Test
        @DisplayName("rejects null id")
        void rejectsNullId() {
            assertThatThrownBy(() -> new Pokemon(null, "pikachu", 112, 4, 60))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("id");
        }

        @Test
        @DisplayName("rejects zero id")
        void rejectsZeroId() {
            assertThatThrownBy(() -> new Pokemon(0, "pikachu", 112, 4, 60))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("id");
        }

        @Test
        @DisplayName("rejects negative id")
        void rejectsNegativeId() {
            assertThatThrownBy(() -> new Pokemon(-1, "pikachu", 112, 4, 60))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("id");
        }

        @Test
        @DisplayName("rejects null name")
        void rejectsNullName() {
            assertThatThrownBy(() -> new Pokemon(25, null, 112, 4, 60))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name");
        }

        @Test
        @DisplayName("rejects blank name")
        void rejectsBlankName() {
            assertThatThrownBy(() -> new Pokemon(25, "   ", 112, 4, 60))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name");
        }

        @Test
        @DisplayName("rejects empty name")
        void rejectsEmptyName() {
            assertThatThrownBy(() -> new Pokemon(25, "", 112, 4, 60))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name");
        }

        @Test
        @DisplayName("rejects negative height")
        void rejectsNegativeHeight() {
            assertThatThrownBy(() -> new Pokemon(25, "pikachu", 112, -1, 60))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("height");
        }

        @Test
        @DisplayName("rejects negative weight")
        void rejectsNegativeWeight() {
            assertThatThrownBy(() -> new Pokemon(25, "pikachu", 112, 4, -1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("weight");
        }
    }

    @Nested
    @DisplayName("equality")
    class Equality {

        @Test
        @DisplayName("two Pokemons with same fields are equal")
        void equalByValue() {
            Pokemon p1 = new Pokemon(25, "pikachu", 112, 4, 60);
            Pokemon p2 = new Pokemon(25, "pikachu", 112, 4, 60);

            assertThat(p1).isEqualTo(p2);
            assertThat(p1.hashCode()).isEqualTo(p2.hashCode());
        }

        @Test
        @DisplayName("two Pokemons with different ids are not equal")
        void notEqualWhenDifferentId() {
            Pokemon p1 = new Pokemon(25, "pikachu", 112, 4, 60);
            Pokemon p2 = new Pokemon(26, "pikachu", 112, 4, 60);

            assertThat(p1).isNotEqualTo(p2);
        }
    }
}