package com.alea.pokemon.application.service;

import com.alea.pokemon.domain.model.Pokemon;
import com.alea.pokemon.domain.port.in.SynchronizePokemonUseCase;
import com.alea.pokemon.domain.port.out.PokemonCatalogProvider;
import com.alea.pokemon.domain.port.out.PokemonRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class PokemonSynchronizationService implements SynchronizePokemonUseCase {

    private static final Logger log = LoggerFactory.getLogger(PokemonSynchronizationService.class);

    private final PokemonCatalogProvider catalogProvider;
    private final PokemonRepository repository;
    private final CacheInvalidator cacheInvalidator;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public PokemonSynchronizationService(PokemonCatalogProvider catalogProvider, PokemonRepository repository, CacheInvalidator cacheInvalidator) {
        this.catalogProvider = catalogProvider;
        this.repository = repository;
        this.cacheInvalidator = cacheInvalidator;
    }

    @Override
    public void synchronize() {
        if (!running.compareAndSet(false, true)) {
            log.info("Synchronization already in progress, skipping");
            return;
        }
        try {
            log.info("Starting Pokemon catalog synchronization");
            long offset = repository.count();
            List<Pokemon> newOnes = catalogProvider.fetchSince(offset);
            if (!newOnes.isEmpty()) {
                repository.saveAll(newOnes);
                cacheInvalidator.evictRankings();
                log.info("Synchronized {} new Pokemon and cleared ranking caches", newOnes.size());
            } else {
                log.info("Catalog up to date, nothing to sync");
            }
        } catch (Exception e) {
            log.error("Synchronization failed, keeping existing data", e);
        } finally {
            running.set(false);
        }
    }
}