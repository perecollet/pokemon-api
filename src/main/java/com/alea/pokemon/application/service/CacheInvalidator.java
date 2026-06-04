package com.alea.pokemon.application.service;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Component;

@Component
public class CacheInvalidator {

    @CacheEvict(value = {"topByWeight", "topByHeight", "topByBaseExperience"}, allEntries = true)
    public void evictRankings() {}
}