package com.alea.pokemon.infrastructure.scheduler;

import com.alea.pokemon.domain.port.in.SynchronizePokemonUseCase;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "alea.pokeapi.sync.enabled", havingValue = "true", matchIfMissing = true)
public class PokemonSyncScheduler implements ApplicationRunner {

    private final SynchronizePokemonUseCase syncUseCase;

    public PokemonSyncScheduler(SynchronizePokemonUseCase syncUseCase) {
        this.syncUseCase = syncUseCase;
    }

    @Override
    public void run(ApplicationArguments args) {
        syncUseCase.synchronize();
    }

    @Scheduled(fixedDelayString = "${alea.pokeapi.sync.interval:PT24H}")
    public void scheduledSync() {
        syncUseCase.synchronize();
    }
}