package com.invest.adapters.scheduler;

import com.invest.domain.ports.in.EvaluateRulesUseCase;
import com.invest.domain.ports.in.SendPendingAlertsUseCase;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
public class RuleEvaluationJob implements Job {

    @Autowired
    private EvaluateRulesUseCase evaluateRulesUseCase;

    @Autowired
    private SendPendingAlertsUseCase sendPendingAlertsUseCase;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.info("M=execute, I=Iniciando job de avaliacao de regras");
        try {
            evaluateRulesUseCase.execute();
            sendPendingAlertsUseCase.execute();
            log.info("M=execute, I=Job de avaliacao de regras concluido com sucesso");
        } catch (Exception exception) {
            log.error("M=execute, E=Erro durante execucao do job de avaliacao de regras", exception);
            throw new JobExecutionException(exception);
        }
    }
}
