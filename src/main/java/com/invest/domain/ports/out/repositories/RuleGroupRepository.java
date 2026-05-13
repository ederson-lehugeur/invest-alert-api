package com.invest.domain.ports.out.repositories;

import com.invest.domain.entities.RuleGroup;

import java.util.List;

public interface RuleGroupRepository {

    RuleGroup save(RuleGroup grupo);

    List<RuleGroup> findAllWithRules();

    List<RuleGroup> findByUserId(Long usuarioId);
}
