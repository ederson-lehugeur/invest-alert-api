package com.invest.infrastructure.config;

import com.invest.application.usecases.UpdateRuleUseCaseImpl;
import com.invest.application.usecases.AuthenticateUserUseCaseImpl;
import com.invest.application.usecases.GetAssetUseCaseImpl;
import com.invest.application.usecases.CreateRuleGroupUseCaseImpl;
import com.invest.application.usecases.CreateRuleUseCaseImpl;
import com.invest.application.usecases.DeleteRuleUseCaseImpl;
import com.invest.application.usecases.ListAssetsUseCaseImpl;
import com.invest.application.usecases.ListRuleGroupsUseCaseImpl;
import com.invest.application.usecases.ListAlertHistoryUseCaseImpl;
import com.invest.application.usecases.ListRulesUseCaseImpl;
import com.invest.application.usecases.RegisterUserUseCaseImpl;
import com.invest.domain.ports.in.UpdateRuleUseCase;
import com.invest.domain.ports.in.AuthenticateUserUseCase;
import com.invest.domain.ports.in.GetAssetUseCase;
import com.invest.domain.ports.in.CreateRuleGroupUseCase;
import com.invest.domain.ports.in.CreateRuleUseCase;
import com.invest.domain.ports.in.DeleteRuleUseCase;
import com.invest.domain.ports.in.ListAssetsUseCase;
import com.invest.domain.ports.in.ListRuleGroupsUseCase;
import com.invest.domain.ports.in.ListAlertHistoryUseCase;
import com.invest.domain.ports.in.ListRulesUseCase;
import com.invest.domain.ports.in.RegisterUserUseCase;
import com.invest.domain.ports.out.repositories.AlertRepository;
import com.invest.domain.ports.out.repositories.AssetRepository;
import com.invest.domain.ports.out.repositories.RuleGroupRepository;
import com.invest.domain.ports.out.PasswordEncoder;
import com.invest.domain.ports.out.repositories.RuleRepository;
import com.invest.domain.ports.out.TokenProvider;
import com.invest.domain.ports.out.repositories.UserRepository;
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
    public ListRuleGroupsUseCase listRuleGroupsUseCase(RuleGroupRepository ruleGroupRepository,
                                                       AlertRepository alertRepository) {
        return new ListRuleGroupsUseCaseImpl(ruleGroupRepository, alertRepository);
    }

    @Bean
    public ListAlertHistoryUseCase listAlertHistoryUseCase(AlertRepository alertRepository) {
        return new ListAlertHistoryUseCaseImpl(alertRepository);
    }
}
