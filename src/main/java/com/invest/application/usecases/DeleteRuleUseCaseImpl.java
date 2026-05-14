package com.invest.application.usecases;

import com.invest.application.ports.in.DeleteRuleUseCase;
import com.invest.domain.entities.Rule;
import com.invest.domain.exceptions.AccessDeniedException;
import com.invest.domain.exceptions.RuleAlreadyTriggeredException;
import com.invest.domain.exceptions.RuleNotFoundException;
import com.invest.domain.ports.out.repositories.AlertRepository;
import com.invest.domain.ports.out.repositories.RuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class DeleteRuleUseCaseImpl implements DeleteRuleUseCase {

    private final RuleRepository ruleRepository;
    private final AlertRepository alertRepository;

    @Override
    public void execute(Long userId, Long ruleId) {
        log.info("M=execute, I=Excluindo regra, userId={}, ruleId={}", userId, ruleId);

        Rule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> {
                    log.warn("M=execute, W=Regra nao encontrada, ruleId={}", ruleId);
                    return new RuleNotFoundException(ruleId);
                });

        if (!rule.getUserId().equals(userId)) {
            log.warn("M=execute, W=Acesso negado, ruleId={}, userId={}", ruleId, userId);
            throw new AccessDeniedException("Access denied: rule does not belong to the authenticated user");
        }

        if (alertRepository.existsByRuleId(ruleId)) {
            log.warn("M=execute, W=Regra ja acionada, ruleId={}", ruleId);
            throw new RuleAlreadyTriggeredException(ruleId);
        }

        ruleRepository.delete(ruleId);
        log.info("M=execute, I=Regra excluida com sucesso, ruleId={}", ruleId);
    }
}
