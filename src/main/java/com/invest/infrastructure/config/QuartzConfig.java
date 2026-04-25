package com.invest.infrastructure.config;

import com.invest.adapters.scheduler.RuleEvaluationJob;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuartzConfig {

    private final long evaluationIntervalMs;

    public QuartzConfig(@Value("${app.scheduler.evaluation-interval-ms}") long evaluationIntervalMs) {
        this.evaluationIntervalMs = evaluationIntervalMs;
    }

    @Bean
    public JobDetail ruleEvaluationJobDetail() {
        return JobBuilder.newJob(RuleEvaluationJob.class)
                .withIdentity("ruleEvaluationJob", "evaluation")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger ruleEvaluationTrigger(JobDetail ruleEvaluationJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(ruleEvaluationJobDetail)
                .withIdentity("ruleEvaluationTrigger", "evaluation")
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInMilliseconds(evaluationIntervalMs)
                        .repeatForever())
                .build();
    }
}
