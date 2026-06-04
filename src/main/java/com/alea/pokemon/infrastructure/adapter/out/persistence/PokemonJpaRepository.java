package com.alea.pokemon.infrastructure.adapter.out.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PokemonJpaRepository extends JpaRepository<PokemonEntity, Integer> {

    List<PokemonEntity> findAllByOrderByWeightDesc(Pageable pageable);

    List<PokemonEntity> findAllByOrderByHeightDesc(Pageable pageable);

    @Query("SELECT p FROM PokemonEntity p ORDER BY p.baseExperience DESC NULLS LAST")
    List<PokemonEntity> findAllByBaseExperienceDescNullsLast(Pageable pageable);
}