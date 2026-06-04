package com.alea.pokemon.infrastructure.scheduler;

import com.alea.pokemon.domain.port.in.SynchronizePokemonUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("PokemonSyncScheduler")
class PokemonSyncSchedulerTest {

    @Mock
    private SynchronizePokemonUseCase syncUseCase;

    @InjectMocks
    private PokemonSyncScheduler scheduler;

    @Test
    @DisplayName("triggers synchronization on application startup")
    void runsOnStartup() {
        scheduler.run(null);

        verify(syncUseCase).synchronize();
    }

    @Test
    @DisplayName("triggers synchronization on schedule")
    void runsOnSchedule() {
        scheduler.scheduledSync();

        verify(syncUseCase).synchronize();
    }
}