package com.alea.pokemon.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.availability.AvailabilityChangeEvent;

import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CatalogReadinessGate")
class CatalogReadinessGateTest {

    @Test
    @DisplayName("is not ready and times out before receiving the LOADED event")
    void notReadyByDefault() {
        CatalogReadinessGate gate = new CatalogReadinessGate(0);

        assertThat(gate.isReady()).isFalse();
        assertThat(gate.awaitReady()).isFalse();
    }

    @Test
    @DisplayName("becomes ready after a LOADED availability event")
    void readyAfterLoadedEvent() {
        CatalogReadinessGate gate = new CatalogReadinessGate(1);

        gate.onAvailabilityChange(new AvailabilityChangeEvent<>(this, CatalogState.LOADED));

        assertThat(gate.isReady()).isTrue();
        assertThat(gate.awaitReady()).isTrue();
    }

    @Test
    @DisplayName("ignores a NOT_LOADED event")
    void ignoresNotLoaded() {
        CatalogReadinessGate gate = new CatalogReadinessGate(0);

        gate.onAvailabilityChange(new AvailabilityChangeEvent<>(this, CatalogState.NOT_LOADED));

        assertThat(gate.isReady()).isFalse();
    }

    @Test
    @DisplayName("releases a blocked caller as soon as LOADED arrives")
    void releasesBlockedCaller() throws Exception {
        CatalogReadinessGate gate = new CatalogReadinessGate(5);
        CountDownLatch callerBlocked = new CountDownLatch(1);

        Thread opener = new Thread(() -> {
            try {
                callerBlocked.await();
                Thread.sleep(50);
                gate.onAvailabilityChange(new AvailabilityChangeEvent<>(this, CatalogState.LOADED));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        opener.start();

        callerBlocked.countDown();
        boolean ready = gate.awaitReady();
        opener.join();

        assertThat(ready).isTrue();
    }
}