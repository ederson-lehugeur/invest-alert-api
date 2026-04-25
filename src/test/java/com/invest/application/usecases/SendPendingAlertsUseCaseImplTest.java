package com.invest.application.usecases;

import com.invest.application.EmailContentBuilder;
import com.invest.domain.entities.Alert;
import com.invest.domain.entities.Asset;
import com.invest.domain.entities.RuleField;
import com.invest.domain.entities.RuleGroup;
import com.invest.domain.entities.ComparisonOperator;
import com.invest.domain.entities.Rule;
import com.invest.domain.entities.AlertStatus;
import com.invest.domain.entities.User;
import com.invest.domain.ports.out.AlertRepository;
import com.invest.domain.ports.out.AssetRepository;
import com.invest.domain.ports.out.EmailGateway;
import com.invest.domain.ports.out.RuleGroupRepository;
import com.invest.domain.ports.out.RuleRepository;
import com.invest.domain.ports.out.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SendPendingAlertsUseCaseImplTest {

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AssetRepository assetRepository;

    @Mock
    private RuleRepository ruleRepository;

    @Mock
    private RuleGroupRepository ruleGroupRepository;

    @Mock
    private EmailGateway emailGateway;

    @Mock
    private EmailContentBuilder emailContentBuilder;

    private SendPendingAlertsUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new SendPendingAlertsUseCaseImpl(
                alertRepository, userRepository, assetRepository,
                ruleRepository, ruleGroupRepository, emailGateway, emailContentBuilder);
    }

    @Test
    void shouldSendEmailAndMarkAlertAsSent_whenSendingSucceeds() {
        Alert alert = buildPendingAlert(1L, 10L, 5L, null, "XPLG11");
        User user = buildUser(10L, "[email protected]");
        Asset asset = buildAsset("XPLG11");
        Rule rule = buildRule(5L);

        when(alertRepository.findPending()).thenReturn(List.of(alert));
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(assetRepository.findByTicker("XPLG11")).thenReturn(Optional.of(asset));
        when(ruleRepository.findById(5L)).thenReturn(Optional.of(rule));
        when(emailContentBuilder.buildSubject(asset)).thenReturn("Alert - XPLG11");
        when(emailContentBuilder.buildBody(eq(asset), eq(rule), any(LocalDateTime.class)))
                .thenReturn("Email body");

        useCase.execute();

        verify(emailGateway).send("[email protected]", "Alert - XPLG11", "Email body");

        ArgumentCaptor<Alert> captor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository).save(captor.capture());
        assertEquals(AlertStatus.SENT, captor.getValue().getStatus());
        assertNotNull(captor.getValue().getSentAt());
    }

    @Test
    void shouldKeepAlertAsPending_whenEmailSendingFails() {
        Alert alert = buildPendingAlert(1L, 10L, 5L, null, "XPLG11");
        User user = buildUser(10L, "[email protected]");
        Asset asset = buildAsset("XPLG11");
        Rule rule = buildRule(5L);

        when(alertRepository.findPending()).thenReturn(List.of(alert));
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(assetRepository.findByTicker("XPLG11")).thenReturn(Optional.of(asset));
        when(ruleRepository.findById(5L)).thenReturn(Optional.of(rule));
        when(emailContentBuilder.buildSubject(asset)).thenReturn("Alert - XPLG11");
        when(emailContentBuilder.buildBody(eq(asset), eq(rule), any(LocalDateTime.class)))
                .thenReturn("Email body");
        doThrow(new RuntimeException("SMTP connection failed"))
                .when(emailGateway).send(anyString(), anyString(), anyString());

        useCase.execute();

        assertEquals(AlertStatus.PENDING, alert.getStatus());
        assertNull(alert.getSentAt());
        verify(alertRepository, never()).save(any());
    }

    @Test
    void shouldContinueSendingOtherAlerts_whenOneAlertFails() {
        Alert failingAlert = buildPendingAlert(1L, 10L, 5L, null, "XPLG11");
        Alert successAlert = buildPendingAlert(2L, 20L, 6L, null, "HGLG11");
        User user1 = buildUser(10L, "[email protected]");
        User user2 = buildUser(20L, "[email protected]");
        Asset asset1 = buildAsset("XPLG11");
        Asset asset2 = buildAsset("HGLG11");
        Rule rule1 = buildRule(5L);
        Rule rule2 = buildRule(6L);

        when(alertRepository.findPending()).thenReturn(List.of(failingAlert, successAlert));
        when(userRepository.findById(10L)).thenReturn(Optional.of(user1));
        when(userRepository.findById(20L)).thenReturn(Optional.of(user2));
        when(assetRepository.findByTicker("XPLG11")).thenReturn(Optional.of(asset1));
        when(assetRepository.findByTicker("HGLG11")).thenReturn(Optional.of(asset2));
        when(ruleRepository.findById(5L)).thenReturn(Optional.of(rule1));
        when(ruleRepository.findById(6L)).thenReturn(Optional.of(rule2));
        when(emailContentBuilder.buildSubject(any(Asset.class))).thenReturn("Subject");
        when(emailContentBuilder.buildBody(any(Asset.class), any(Rule.class), any(LocalDateTime.class)))
                .thenReturn("Body");

        doThrow(new RuntimeException("SMTP error"))
                .doNothing()
                .when(emailGateway).send(anyString(), anyString(), anyString());

        useCase.execute();

        assertEquals(AlertStatus.PENDING, failingAlert.getStatus());
        verify(alertRepository, times(1)).save(any(Alert.class));
    }

    @Test
    void shouldSendEmailForGroupAlert() {
        Alert alert = buildPendingAlert(1L, 10L, null, 100L, "HGLG11");
        User user = buildUser(10L, "[email protected]");
        Asset asset = buildAsset("HGLG11");
        RuleGroup group = new RuleGroup(100L, 10L, "HGLG11", "Grupo FII",
                List.of(buildRule(1L)), LocalDateTime.now());

        when(alertRepository.findPending()).thenReturn(List.of(alert));
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(assetRepository.findByTicker("HGLG11")).thenReturn(Optional.of(asset));
        when(ruleGroupRepository.findAllWithRules()).thenReturn(List.of(group));
        when(emailContentBuilder.buildSubject(asset)).thenReturn("Alert Grupo");
        when(emailContentBuilder.buildBody(eq(asset), eq(group), any(LocalDateTime.class)))
                .thenReturn("Group email body");

        useCase.execute();

        verify(emailGateway).send("[email protected]", "Alert Grupo", "Group email body");
        verify(alertRepository).save(alert);
        assertEquals(AlertStatus.SENT, alert.getStatus());
    }

    @Test
    void shouldDoNothing_whenNoPendingAlertsExist() {
        when(alertRepository.findPending()).thenReturn(List.of());

        useCase.execute();

        verify(emailGateway, never()).send(anyString(), anyString(), anyString());
        verify(alertRepository, never()).save(any());
    }

    @Test
    void shouldSkipAlert_whenUserNotFound() {
        Alert alert = buildPendingAlert(1L, 999L, 5L, null, "XPLG11");

        when(alertRepository.findPending()).thenReturn(List.of(alert));
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        useCase.execute();

        verify(emailGateway, never()).send(anyString(), anyString(), anyString());
        verify(alertRepository, never()).save(any());
        assertEquals(AlertStatus.PENDING, alert.getStatus());
    }

    @Test
    void shouldSkipAlert_whenAssetNotFound() {
        Alert alert = buildPendingAlert(1L, 10L, 5L, null, "MISSING");
        User user = buildUser(10L, "[email protected]");

        when(alertRepository.findPending()).thenReturn(List.of(alert));
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(assetRepository.findByTicker("MISSING")).thenReturn(Optional.empty());

        useCase.execute();

        verify(emailGateway, never()).send(anyString(), anyString(), anyString());
        verify(alertRepository, never()).save(any());
        assertEquals(AlertStatus.PENDING, alert.getStatus());
    }

    private Alert buildPendingAlert(Long id, Long userId, Long ruleId, Long groupId, String ticker) {
        return new Alert(id, userId, ruleId, groupId, ticker,
                AlertStatus.PENDING, null, LocalDateTime.now(), null);
    }

    private User buildUser(Long id, String email) {
        return new User(id, "Test User", email, "hashedPassword",
                LocalDateTime.now(), LocalDateTime.now());
    }

    private Asset buildAsset(String ticker) {
        return new Asset(1L, ticker, "FII " + ticker, BigDecimal.valueOf(100),
                BigDecimal.valueOf(8.5), BigDecimal.valueOf(0.95), LocalDateTime.now());
    }

    private Rule buildRule(Long id) {
        return new Rule(id, 10L, "XPLG11", null, RuleField.PRICE,
                ComparisonOperator.LESS_THAN, BigDecimal.valueOf(120),
                true, LocalDateTime.now(), LocalDateTime.now());
    }
}
