package com.alea.pokemon.infrastructure.scheduler;

import com.alea.pokemon.application.service.PokemonSynchronizationService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "alea.pokeapi.sync.enabled", havingValue = "true", matchIfMissing = true)
public class PokemonSyncScheduler implements ApplicationRunner {

    private final PokemonSynchronizationService syncService;

    public PokemonSyncScheduler(PokemonSynchronizationService syncService) {
        this.syncService = syncService;
    }

    @Override
    public void run(ApplicationArguments args) {
        syncService.synchronize();
    }

    @Scheduled(fixedDelayString = "${alea.pokeapi.sync.interval:PT24H}")
    public void scheduledSync() {
        syncService.synchronize();
    }
}