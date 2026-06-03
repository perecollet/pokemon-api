package com.alea.pokemon.infrastructure.adapter.out.persistence;

import com.alea.pokemon.domain.model.Pokemon;
import com.alea.pokemon.domain.port.out.PokemonRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class JpaPokemonRepository implements PokemonRepository {

    private final PokemonJpaRepository jpaRepository;
    private final PokemonEntityMapper mapper;

    public JpaPokemonRepository(PokemonJpaRepository jpaRepository, PokemonEntityMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public void saveAll(List<Pokemon> pokemons) {
        List<PokemonEntity> entities = pokemons.stream()
                .map(mapper::toEntity)
                .toList();
        jpaRepository.saveAll(entities);
    }

    @Override
    public List<Pokemon> findTopByWeight(int limit) {
        return jpaRepository.findTop5ByOrderByWeightDesc().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Pokemon> findTopByHeight(int limit) {
        return jpaRepository.findTop5ByOrderByHeightDesc().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Pokemon> findTopByBaseExperience(int limit) {
        return jpaRepository.findTop5ByBaseExperienceDescNullsLast(PageRequest.of(0, 5)).stream()
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