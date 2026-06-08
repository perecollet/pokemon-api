package com.alea.pokemon.application.service;

import com.alea.pokemon.domain.model.Pokemon;
import com.alea.pokemon.domain.port.out.PokemonCatalogProvider;
import com.alea.pokemon.domain.port.out.PokemonRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(
        classes = {
                RankingCacheReader.class,
                PokemonSynchronizationService.class,
                CacheInvalidator.class,
                org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration.class
        },
        properties = {
                "spring.cache.type=caffeine",
                "spring.cache.caffeine.spec=maximumSize=10,expireAfterWrite=24h,recordStats"
        }
)
@EnableCaching
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

    @Autowired
    private CacheManager cacheManager;

    private final Pokemon pikachu = new Pokemon(25, "pikachu", 112, 4, 60);
    private final Pokemon charizard = new Pokemon(6, "charizard", 267, 17, 905);

    @BeforeEach
    void setup() {
        cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
        when(repository.findTopByWeight(50)).thenReturn(List.of(charizard, pikachu));
        when(repository.findTopByHeight(50)).thenReturn(List.of(charizard, pikachu));
        when(repository.findTopByBaseExperience(50)).thenReturn(List.of(charizard, pikachu));
    }

    @Test
    @DisplayName("caches heaviest results")
    void cachesHeaviest() {
        cacheReader.heaviest();
        cacheReader.heaviest();
        cacheReader.heaviest();

        verify(repository, times(1)).findTopByWeight(50);
    }

    @Test
    @DisplayName("caches tallest results")
    void cachesTallest() {
        cacheReader.tallest();
        cacheReader.tallest();

        verify(repository, times(1)).findTopByHeight(50);
    }

    @Test
    @DisplayName("caches most experienced results")
    void cachesMostExperienced() {
        cacheReader.mostExperienced();
        cacheReader.mostExperienced();

        verify(repository, times(1)).findTopByBaseExperience(50);
    }

    @Test
    @DisplayName("synchronize evicts all ranking caches when new pokemons arrive")
    void evictsAllCachesOnSync() {
        when(repository.count()).thenReturn(0L);
        when(catalogProvider.fetchSince(0L)).thenReturn(List.of(pikachu));

        cacheReader.heaviest();
        cacheReader.tallest();
        cacheReader.mostExperienced();

        syncService.synchronize();

        cacheReader.heaviest();
        cacheReader.tallest();
        cacheReader.mostExperienced();

        verify(repository, times(2)).findTopByWeight(50);
        verify(repository, times(2)).findTopByHeight(50);
        verify(repository, times(2)).findTopByBaseExperience(50);
    }

    @Test
    @DisplayName("cached list contains the expected pokemons")
    void cachedListContent() {
        List<Pokemon> result = cacheReader.heaviest();

        assertThat(result).containsExactly(charizard, pikachu);
    }
}