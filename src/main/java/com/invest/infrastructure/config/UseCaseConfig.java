package com.invest.infrastructure.config;

import com.invest.application.EmailContentBuilder;
import com.invest.application.usecases.UpdateRuleUseCaseImpl;
import com.invest.application.usecases.AuthenticateUserUseCaseImpl;
import com.invest.application.usecases.EvaluateRulesUseCaseImpl;
import com.invest.application.usecases.GetAssetUseCaseImpl;
import com.invest.application.usecases.CreateRuleGroupUseCaseImpl;
import com.invest.application.usecases.CreateRuleUseCaseImpl;
import com.invest.application.usecases.SendPendingAlertsUseCaseImpl;
import com.invest.application.usecases.DeleteRuleUseCaseImpl;
import com.invest.application.usecases.ListAssetsUseCaseImpl;
import com.invest.application.usecases.ListRuleGroupsUseCaseImpl;
import com.invest.application.usecases.ListAlertHistoryUseCaseImpl;
import com.invest.application.usecases.ListRulesUseCaseImpl;
import com.invest.application.usecases.RegisterUserUseCaseImpl;
import com.invest.domain.ports.in.UpdateRuleUseCase;
import com.invest.domain.ports.in.AuthenticateUserUseCase;
import com.invest.domain.ports.in.EvaluateRulesUseCase;
import com.invest.domain.ports.in.GetAssetUseCase;
import com.invest.domain.ports.in.CreateRuleGroupUseCase;
import com.invest.domain.ports.in.CreateRuleUseCase;
import com.invest.domain.ports.in.SendPendingAlertsUseCase;
import com.invest.domain.ports.in.DeleteRuleUseCase;
import com.invest.domain.ports.in.ListAssetsUseCase;
import com.invest.domain.ports.in.ListRuleGroupsUseCase;
import com.invest.domain.ports.in.ListAlertHistoryUseCase;
import com.invest.domain.ports.in.ListRulesUseCase;
import com.invest.domain.ports.in.RegisterUserUseCase;
import com.invest.domain.ports.out.AlertRepository;
import com.invest.domain.ports.out.AssetRepository;
import com.invest.domain.ports.out.EmailGateway;
import com.invest.domain.ports.out.RuleGroupRepository;
import com.invest.domain.ports.out.PasswordEncoder;
import com.invest.domain.ports.out.RuleRepository;
import com.invest.domain.ports.out.TokenProvider;
import com.invest.domain.ports.out.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseConfig {

    @Bean
    public RegisterUserUseCase registerUserUseCase(UserRepository userRepository,
                                                   PasswordEncoder passwordEncoder) {
        return new RegisterUserUseCaseImpl(userRepository, passwordEncoder);
    }

    @Bean
    public AuthenticateUserUseCase authenticateUserUseCase(UserRepository userRepository,
                                                           PasswordEncoder passwordEncoder,
                                                           TokenProvider tokenProvider) {
        return new AuthenticateUserUseCaseImpl(userRepository, passwordEncoder, tokenProvider);
    }

    @Bean
    public ListAssetsUseCase listAssetsUseCase(AssetRepository assetRepository) {
        return new ListAssetsUseCaseImpl(assetRepository);
    }

    @Bean
    public GetAssetUseCase getAssetUseCase(AssetRepository assetRepository) {
        return new GetAssetUseCaseImpl(assetRepository);
    }

    @Bean
    public CreateRuleUseCase createRuleUseCase(RuleRepository ruleRepository,
                                               AssetRepository assetRepository) {
        return new CreateRuleUseCaseImpl(ruleRepository, assetRepository);
    }

    @Bean
    public UpdateRuleUseCase updateRuleUseCase(RuleRepository ruleRepository,
                                               AlertRepository alertRepository) {
        return new UpdateRuleUseCaseImpl(ruleRepository, alertRepository);
    }

    @Bean
    public DeleteRuleUseCase deleteRuleUseCase(RuleRepository ruleRepository,
                                               AlertRepository alertRepository) {
        return new DeleteRuleUseCaseImpl(ruleRepository, alertRepository);
    }

    @Bean
    public ListRulesUseCase listRulesUseCase(RuleRepository ruleRepository,
                                             AlertRepository alertRepository) {
        return new ListRulesUseCaseImpl(ruleRepository, alertRepository);
    }

    @Bean
    public CreateRuleGroupUseCase createRuleGroupUseCase(RuleGroupRepository ruleGroupRepository,
                                                         AssetRepository assetRepository) {
        return new CreateRuleGroupUseCaseImpl(ruleGroupRepository, assetRepository);
    }

    @Bean
    public ListRuleGroupsUseCase listRuleGroupsUseCase(RuleGroupRepository ruleGroupRepository) {
        return new ListRuleGroupsUseCaseImpl(ruleGroupRepository);
    }

    @Bean
    public EvaluateRulesUseCase evaluateRulesUseCase(RuleRepository ruleRepository,
                                                     RuleGroupRepository ruleGroupRepository,
                                                     AssetRepository assetRepository,
                                                     AlertRepository alertRepository) {
        return new EvaluateRulesUseCaseImpl(ruleRepository, ruleGroupRepository, assetRepository, alertRepository);
    }

    @Bean
    public SendPendingAlertsUseCase sendPendingAlertsUseCase(AlertRepository alertRepository,
                                                             UserRepository userRepository,
                                                             AssetRepository assetRepository,
                                                             RuleRepository ruleRepository,
                                                             RuleGroupRepository ruleGroupRepository,
                                                             EmailGateway emailGateway,
                                                             EmailContentBuilder emailContentBuilder) {
        return new SendPendingAlertsUseCaseImpl(alertRepository, userRepository, assetRepository,
                ruleRepository, ruleGroupRepository, emailGateway, emailContentBuilder);
    }

    @Bean
    public ListAlertHistoryUseCase listAlertHistoryUseCase(AlertRepository alertRepository) {
        return new ListAlertHistoryUseCaseImpl(alertRepository);
    }

    @Bean
    public EmailContentBuilder emailContentBuilder() {
        return new EmailContentBuilder();
    }
}
