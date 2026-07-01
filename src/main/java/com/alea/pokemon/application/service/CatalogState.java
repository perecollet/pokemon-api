package com.alea.pokemon.application.service;
import org.springframework.boot.availability.AvailabilityState;

public enum CatalogState implements AvailabilityState {
    LOADED,
    NOT_LOADED
}