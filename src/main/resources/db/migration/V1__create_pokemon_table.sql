CREATE TABLE pokemon (
                         id              INTEGER PRIMARY KEY,
                         name            VARCHAR(100) NOT NULL,
                         base_experience INTEGER,
                         height          INTEGER NOT NULL,
                         weight          INTEGER NOT NULL,
                         last_updated    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_pokemon_weight ON pokemon (weight DESC);
CREATE INDEX idx_pokemon_height ON pokemon (height DESC);
CREATE INDEX idx_pokemon_base_experience ON pokemon (base_experience DESC NULLS LAST);

COMMENT ON COLUMN pokemon.weight IS 'Weight in hectograms (PokéAPI native unit)';
COMMENT ON COLUMN pokemon.height IS 'Height in decimeters (PokéAPI native unit)';