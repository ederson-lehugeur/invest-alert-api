package com.invest.domain.ports.in;

import com.invest.application.responses.RuleResponse;

import java.util.List;

public interface ListRulesUseCase {

    List<RuleResponse> execute(Long usuarioId);
}
