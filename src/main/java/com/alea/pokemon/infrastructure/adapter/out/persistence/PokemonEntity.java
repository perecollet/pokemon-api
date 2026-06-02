package com.alea.pokemon.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "pokemon")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PokemonEntity {

    @Id
    private Integer id;

    @Column(nullable = false)
    private String name;

    @Column(name = "base_experience")
    private Integer baseExperience;

    @Column(nullable = false)
    private Integer height;

    @Column(nullable = false)
    private Integer weight;

    @Column(name = "last_updated", nullable = false)
    private Instant lastUpdated;
}