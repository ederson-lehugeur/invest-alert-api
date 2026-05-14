package com.invest.infrastructure.config;

import com.invest.application.usecases.AuthenticateUserUseCaseImpl;
import com.invest.application.usecases.CreateRuleGroupUseCaseImpl;
import com.invest.application.usecases.CreateRuleUseCaseImpl;
import com.invest.application.usecases.DeleteRuleUseCaseImpl;
import com.invest.application.usecases.GetAssetUseCaseImpl;
import com.invest.application.usecases.ListAlertHistoryUseCaseImpl;
import com.invest.application.usecases.ListAssetsUseCaseImpl;
import com.invest.application.usecases.ListRuleGroupsUseCaseImpl;
import com.invest.application.usecases.ListRulesUseCaseImpl;
import com.invest.application.usecases.RefreshTokenUseCaseImpl;
import com.invest.application.usecases.RegisterUserUseCaseImpl;
import com.invest.application.usecases.RevokeRefreshTokenUseCaseImpl;
import com.invest.application.usecases.UpdateRuleUseCaseImpl;
import com.invest.application.ports.in.AuthenticateUserUseCase;
import com.invest.application.ports.in.CreateRuleGroupUseCase;
import com.invest.application.ports.in.CreateRuleUseCase;
import com.invest.application.ports.in.DeleteRuleUseCase;
import com.invest.application.ports.in.GetAssetUseCase;
import com.invest.application.ports.in.ListAlertHistoryUseCase;
import com.invest.application.ports.in.ListAssetsUseCase;
import com.invest.application.ports.in.ListRuleGroupsUseCase;
import com.invest.application.ports.in.ListRulesUseCase;
import com.invest.application.ports.in.RefreshTokenUseCase;
import com.invest.application.ports.in.RegisterUserUseCase;
import com.invest.application.ports.in.RevokeRefreshTokenUseCase;
import com.invest.application.ports.in.UpdateRuleUseCase;
import com.invest.domain.ports.out.PasswordEncoder;
import com.invest.domain.ports.out.RefreshTokenGenerator;
import com.invest.domain.ports.out.TokenProvider;
import com.invest.domain.ports.out.repositories.AlertRepository;
import com.invest.domain.ports.out.repositories.AssetRepository;
import com.invest.domain.ports.out.repositories.RefreshTokenRepository;
import com.invest.domain.ports.out.repositories.RoleRepository;
import com.invest.domain.ports.out.repositories.RuleGroupRepository;
import com.invest.domain.ports.out.repositories.RuleRepository;
import com.invest.domain.ports.out.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseConfig {

    @Bean
    public RegisterUserUseCase registerUserUseCase(UserRepository userRepository,
                                                   PasswordEncoder passwordEncoder,
                                                   RoleRepository roleRepository) {
        return new RegisterUserUseCaseImpl(userRepository, passwordEncoder, roleRepository);
    }

    @Bean
    public AuthenticateUserUseCase authenticateUserUseCase(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            TokenProvider tokenProvider,
            RefreshTokenRepository refreshTokenRepository,
            RefreshTokenGenerator refreshTokenGenerator,
            @Value("${app.jwt.refresh-token-expiration-seconds}") long refreshTokenExpirationSeconds) {
        return new AuthenticateUserUseCaseImpl(
                userRepository, passwordEncoder, tokenProvider,
                refreshTokenRepository, refreshTokenGenerator, refreshTokenExpirationSeconds);
    }

    @Bean
    public RefreshTokenUseCase refreshTokenUseCase(
            RefreshTokenRepository refreshTokenRepository,
            UserRepository userRepository,
            TokenProvider tokenProvider,
            RefreshTokenGenerator refreshTokenGenerator,
            @Value("${app.jwt.refresh-token-expiration-seconds}") long refreshTokenExpirationSeconds) {
        return new RefreshTokenUseCaseImpl(
                refreshTokenRepository, userRepository, tokenProvider,
                refreshTokenGenerator, refreshTokenExpirationSeconds);
    }

    @Bean
    public RevokeRefreshTokenUseCase revokeRefreshTokenUseCase(RefreshTokenRepository refreshTokenRepository) {
        return new RevokeRefreshTokenUseCaseImpl(refreshTokenRepository);
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
