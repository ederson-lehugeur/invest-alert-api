package com.invest.application.usecases;

import com.invest.domain.exceptions.RuleAlreadyTriggeredException;
import com.invest.domain.exceptions.RuleNotFoundException;
import com.invest.domain.ports.in.DeleteRuleUseCase;
import com.invest.domain.ports.out.AlertRepository;
import com.invest.domain.ports.out.RuleRepository;
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

        ruleRepository.findByIdAndUserId(ruleId, userId)
                .orElseThrow(() -> {
                    log.warn("M=execute, W=Regra nao encontrada, ruleId={}, userId={}", ruleId, userId);
                    return new RuleNotFoundException(ruleId);
                });

        if (alertRepository.existsByRuleId(ruleId)) {
            log.warn("M=execute, W=Regra ja acionada, ruleId={}", ruleId);
            throw new RuleAlreadyTriggeredException(ruleId);
        }

        ruleRepository.delete(ruleId);
        log.info("M=execute, I=Regra excluida com sucesso, ruleId={}", ruleId);
    }
}
