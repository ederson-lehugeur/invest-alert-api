package com.invest.domain.ports.out.repositories;

import com.invest.domain.entities.Rule;

import java.util.List;
import java.util.Optional;

public interface RuleRepository {

    Rule save(Rule regra);

    Optional<Rule> findById(Long regraId);

    Optional<Rule> findByIdAndUserId(Long regraId, Long usuarioId);

    List<Rule> findByUserId(Long usuarioId);

    List<Rule> findAllActive();

    List<Rule> findByGroupId(Long groupId);

    void delete(Long regraId);
}
