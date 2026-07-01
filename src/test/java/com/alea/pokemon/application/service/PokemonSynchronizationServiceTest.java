package com.alea.pokemon.application.service;

import com.alea.pokemon.domain.model.Pokemon;
import com.alea.pokemon.domain.port.out.PokemonCatalogProvider;
import com.alea.pokemon.domain.port.out.PokemonRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PokemonSynchronizationService")
class PokemonSynchronizationServiceTest {

    @Mock
    private PokemonCatalogProvider catalogProvider;

    @Mock
    private PokemonRepository repository;

    @Mock
    private CacheInvalidator cacheInvalidator;

    @Mock
    private ApplicationEventPublisher events;

    @InjectMocks
    private PokemonSynchronizationService service;

    private final Pokemon pikachu = new Pokemon(25, "pikachu", 112, 4, 60);

    @Test
    @DisplayName("fetches new Pokémon from current offset and saves them")
    void synchronizesNewPokemon() {
        when(repository.count()).thenReturn(0L);
        when(catalogProvider.fetchSince(0L)).thenReturn(List.of(pikachu));

        service.synchronize();

        verify(repository).saveAll(List.of(pikachu));
    }

    @Test
    @DisplayName("does not save when no new Pokémon available")
    void noOpWhenUpToDate() {
        when(repository.count()).thenReturn(1351L);
        when(catalogProvider.fetchSince(1351L)).thenReturn(List.of());

        service.synchronize();

        verify(repository, never()).saveAll(any());
    }

    @Test
    @DisplayName("does not fail when provider throws")
    void tolerantOfProviderFailure() {
        when(repository.count()).thenReturn(0L);
        when(catalogProvider.fetchSince(0L)).thenThrow(new RuntimeException("PokeAPI down"));

        service.synchronize();

        verify(repository, never()).saveAll(any());
    }

    @Test
    @DisplayName("evicts cache when new Pokémon are synchronized")
    void evictsCacheOnNewData() {
        when(repository.count()).thenReturn(0L);
        when(catalogProvider.fetchSince(0L)).thenReturn(List.of(pikachu));

        service.synchronize();

        verify(cacheInvalidator).evictRankings();
    }

    @Test
    @DisplayName("does not evict cache when there are no new Pokémon")
    void noEvictWhenUpToDate() {
        when(repository.count()).thenReturn(1351L);
        when(catalogProvider.fetchSince(1351L)).thenReturn(List.of());

        service.synchronize();

        verify(cacheInvalidator, never()).evictRankings();
    }

    @Test
    @DisplayName("skips when a synchronization is already in progress")
    void skipsWhenAlreadyRunning() throws Exception {
        CountDownLatch firstStarted = new CountDownLatch(1);
        CountDownLatch firstCanFinish = new CountDownLatch(1);

        when(repository.count()).thenReturn(0L);
        when(catalogProvider.fetchSince(0L)).thenAnswer(inv -> {
            firstStarted.countDown();
            firstCanFinish.await();
            return List.of(pikachu);
        });

        Thread first = new Thread(service::synchronize);
        first.start();
        firstStarted.await();

        service.synchronize();

        firstCanFinish.countDown();
        first.join();

        verify(catalogProvider, times(1)).fetchSince(0L);
    }

    @Test
    @DisplayName("publishes CatalogState.LOADED once the catalog contains data")
    void publishesLoadedWhenDataPresent() {
        when(repository.count()).thenReturn(0L);
        when(catalogProvider.fetchSince(0L)).thenReturn(List.of(pikachu));
        when(repository.isEmpty()).thenReturn(false);

        service.synchronize();

        ArgumentCaptor<AvailabilityChangeEvent> captor = ArgumentCaptor.forClass(AvailabilityChangeEvent.class);
        verify(events).publishEvent(captor.capture());
        assertThat(captor.getValue().getState()).isEqualTo(CatalogState.LOADED);
    }

    @Test
    @DisplayName("does not signal readiness when the catalog stays empty")
    void doesNotSignalWhenStillEmpty() {
        when(repository.count()).thenReturn(0L);
        when(catalogProvider.fetchSince(0L)).thenThrow(new RuntimeException("PokeAPI down"));
        when(repository.isEmpty()).thenReturn(true);

        service.synchronize();

        verify(events, never()).publishEvent(any(AvailabilityChangeEvent.class));
    }
}