package com.alea.pokemon.application.service;

import com.alea.pokemon.domain.model.Pokemon;
import com.alea.pokemon.domain.port.out.PokemonCatalogProvider;
import com.alea.pokemon.domain.port.out.PokemonRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SpringBootTest(
        properties = "alea.pokeapi.sync.enabled=false",
        classes = {
                RankingCacheReader.class,
                PokemonSynchronizationService.class,
                com.alea.pokemon.PokemonApiApplication.class
        }
)
@DisplayName("Cache eviction on synchronization")
class PokemonSyncCacheEvictionTest {

    @MockitoBean
    private PokemonRepository repository;

    @MockitoBean
    private PokemonCatalogProvider catalogProvider;

    @Autowired
    private RankingCacheReader cacheReader;

    @Autowired
    private PokemonSynchronizationService syncService;

    private final Pokemon pikachu = new Pokemon(25, "pikachu", 112, 4, 60);
    private final Pokemon charizard = new Pokemon(6, "charizard", 267, 17, 905);

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void setup() {
        cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
        when(repository.findTopByWeight(50)).thenReturn(List.of(charizard, pikachu));
    }

    @Test
    @DisplayName("repeated calls hit cache without querying the repository again")
    void cachesResults() {
        cacheReader.heaviest();
        cacheReader.heaviest();
        cacheReader.heaviest();

        verify(repository, times(1)).findTopByWeight(50);
    }

    @Test
    @DisplayName("synchronize evicts the cache, forcing a fresh repository call")
    void evictsCacheOnSync() {
        when(repository.count()).thenReturn(0L);
        when(catalogProvider.fetchSince(0L)).thenReturn(List.of(pikachu));

        cacheReader.heaviest();
        cacheReader.heaviest();

        syncService.synchronize();

        cacheReader.heaviest();

        verify(repository, times(2)).findTopByWeight(50);
    }

    @Test
    @DisplayName("cached list contains the expected pokemons")
    void cachedListContent() {
        List<Pokemon> result = cacheReader.heaviest();

        assertThat(result).containsExactly(charizard, pikachu);
    }
}