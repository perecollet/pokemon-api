package com.alea.pokemon.infrastructure.adapter.out.persistence;

import com.alea.pokemon.domain.model.Pokemon;
import com.alea.pokemon.domain.port.out.PokemonRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@Transactional(readOnly = true)
public class JpaPokemonRepository implements PokemonRepository {

    private final PokemonJpaRepository jpaRepository;
    private final PokemonEntityMapper mapper;

    public JpaPokemonRepository(PokemonJpaRepository jpaRepository, PokemonEntityMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public void saveAll(List<Pokemon> pokemons) {
        List<PokemonEntity> entities = pokemons.stream()
                .map(mapper::toEntity)
                .toList();
        jpaRepository.saveAll(entities);
    }

    @Override
    public List<Pokemon> findTopByWeight(int limit) {
        return jpaRepository.findAllByOrderByWeightDesc(PageRequest.of(0,limit)).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Pokemon> findTopByHeight(int limit) {
        return jpaRepository.findAllByOrderByHeightDesc(PageRequest.of(0,limit)).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Pokemon> findTopByBaseExperience(int limit) {
        return jpaRepository.findAllByBaseExperienceDescNullsLast(PageRequest.of(0,limit)).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public boolean isEmpty() {
        return count() == 0;
    }

    @Override
    public long count() {
        return jpaRepository.count();
    }
}