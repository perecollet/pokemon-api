package com.alea.pokemon.application.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Component
public class CatalogReadinessGate {

    private final CountDownLatch ready = new CountDownLatch(1);
    private final long timeoutSeconds;

    public CatalogReadinessGate(@Value("${alea.pokeapi.readiness-timeout-seconds}") long timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    @EventListener
    public void onAvailabilityChange(AvailabilityChangeEvent<CatalogState> event) {
        if (event.getState() == CatalogState.LOADED) {
            ready.countDown();
        }
    }

    public boolean isReady() {
        return ready.getCount() == 0;
    }

    public boolean awaitReady() {
        try {
            return ready.await(timeoutSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}