package com.alea.pokemon.application.service;

import com.alea.pokemon.domain.model.Pokemon;
import com.alea.pokemon.domain.port.out.PokemonCatalogProvider;
import com.alea.pokemon.domain.port.out.PokemonRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PokemonSynchronizationService {

    private static final Logger log = LoggerFactory.getLogger(PokemonSynchronizationService.class);

    private final PokemonCatalogProvider catalogProvider;
    private final PokemonRepository repository;

    public PokemonSynchronizationService(PokemonCatalogProvider catalogProvider, PokemonRepository repository) {
        this.catalogProvider = catalogProvider;
        this.repository = repository;
    }

    public void synchronize() {
        log.info("Starting Pokemon catalog synchronization");
        try {
            long offset = repository.count();
            List<Pokemon> newOnes = catalogProvider.fetchSince(offset);
            if (!newOnes.isEmpty()) {
                repository.saveAll(newOnes);
                log.info("Synchronized {} new Pokemon", newOnes.size());
            } else {
                log.info("Catalog up to date, nothing to sync");
            }
        } catch (Exception e) {
            log.error("Synchronization failed, keeping existing data", e);
        }
    }
}