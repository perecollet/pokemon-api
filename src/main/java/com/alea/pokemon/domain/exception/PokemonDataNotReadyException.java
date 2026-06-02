package com.alea.pokemon.domain.exception;

public class PokemonDataNotReadyException extends RuntimeException {

    public PokemonDataNotReadyException() {
        super("Pokemon catalog has not been synchronized yet");
    }
}