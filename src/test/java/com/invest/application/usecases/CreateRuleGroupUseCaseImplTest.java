package com.invest.application.usecases;

import com.invest.application.commands.CreateRuleGroupCommand;
import com.invest.application.commands.CreateRuleCommand;
import com.invest.application.responses.RuleGroupResponse;
import com.invest.domain.entities.Asset;
import com.invest.domain.entities.RuleField;
import com.invest.domain.entities.RuleGroup;
import com.invest.domain.entities.ComparisonOperator;
import com.invest.domain.exceptions.AssetNotFoundException;
import com.invest.domain.exceptions.InvalidRuleFieldException;
import com.invest.domain.ports.out.AssetRepository;
import com.invest.domain.ports.out.RuleGroupRepository;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateRuleGroupUseCaseImplTest {

    @Mock
    private RuleGroupRepository ruleGroupRepository;

    @Mock
    private AssetRepository assetRepository;

    private CreateRuleGroupUseCaseImpl useCase;

    private static final Long USER_ID = 10L;
    private static final String TICKER = "XPLG11";

    @BeforeEach
    void setUp() {
        useCase = new CreateRuleGroupUseCaseImpl(ruleGroupRepository, assetRepository);
    }

    private Asset defaultAsset() {
        return new Asset(1L, TICKER, "FII XP Log", BigDecimal.valueOf(110),
                BigDecimal.valueOf(8.5), BigDecimal.valueOf(0.95), LocalDateTime.now());
    }

    private CreateRuleCommand ruleCommand(RuleField field, ComparisonOperator operator, BigDecimal value) {
        return new CreateRuleCommand(TICKER, field, operator, value, null);
    }

    @Test
    void shouldCreateGroupWithValidData() {
        var rules = List.of(
                ruleCommand(RuleField.PRICE, ComparisonOperator.LESS_THAN, BigDecimal.valueOf(100)),
                ruleCommand(RuleField.DIVIDEND_YIELD, ComparisonOperator.GREATER_THAN, BigDecimal.valueOf(8))
        );
        var command = new CreateRuleGroupCommand(TICKER, "Oportunidade XPLG11", rules);

        when(assetRepository.findByTicker(TICKER)).thenReturn(Optional.of(defaultAsset()));
        when(ruleGroupRepository.save(any(RuleGroup.class))).thenAnswer(invocation -> {
            RuleGroup g = invocation.getArgument(0);
            g.setId(1L);
            for (int i = 0; i < g.getRules().size(); i++) {
                g.getRules().get(i).setId((long) (i + 1));
                g.getRules().get(i).setGroupId(1L);
            }
            return g;
        });

        RuleGroupResponse response = useCase.execute(USER_ID, command);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals(TICKER, response.ticker());
        assertEquals("Oportunidade XPLG11", response.name());
        assertEquals(2, response.rules().size());
    }

    @Test
    void shouldSaveGroupWithCorrectUserId() {
        var rules = List.of(
                ruleCommand(RuleField.P_VP, ComparisonOperator.LESS_THAN_OR_EQUAL, BigDecimal.valueOf(1.2))
        );
        var command = new CreateRuleGroupCommand(TICKER, "Grupo PVP", rules);

        when(assetRepository.findByTicker(TICKER)).thenReturn(Optional.of(defaultAsset()));
        when(ruleGroupRepository.save(any(RuleGroup.class))).thenAnswer(invocation -> invocation.getArgument(0));

        useCase.execute(USER_ID, command);

        ArgumentCaptor<RuleGroup> captor = ArgumentCaptor.forClass(RuleGroup.class);
        verify(ruleGroupRepository).save(captor.capture());
        assertEquals(USER_ID, captor.getValue().getUserId());
    }

    @Test
    void shouldAssignGroupTickerToAllRules() {
        var rules = List.of(
                ruleCommand(RuleField.PRICE, ComparisonOperator.LESS_THAN, BigDecimal.valueOf(100)),
                new CreateRuleCommand(null, RuleField.DIVIDEND_YIELD, ComparisonOperator.GREATER_THAN, BigDecimal.valueOf(8), null)
        );
        var command = new CreateRuleGroupCommand(TICKER, "Grupo Misto", rules);

        when(assetRepository.findByTicker(TICKER)).thenReturn(Optional.of(defaultAsset()));
        when(ruleGroupRepository.save(any(RuleGroup.class))).thenAnswer(invocation -> invocation.getArgument(0));

        useCase.execute(USER_ID, command);

        ArgumentCaptor<RuleGroup> captor = ArgumentCaptor.forClass(RuleGroup.class);
        verify(ruleGroupRepository).save(captor.capture());
        RuleGroup saved = captor.getValue();
        saved.getRules().forEach(rule -> assertEquals(TICKER, rule.getTicker()));
    }

    @Test
    void shouldThrowAssetNotFoundException_whenTickerDoesNotExist() {
        var rules = List.of(
                ruleCommand(RuleField.PRICE, ComparisonOperator.LESS_THAN, BigDecimal.valueOf(100))
        );
        var command = new CreateRuleGroupCommand("INVALID", "Grupo", rules);

        when(assetRepository.findByTicker("INVALID")).thenReturn(Optional.empty());

        assertThrows(AssetNotFoundException.class, () -> useCase.execute(USER_ID, command));
        verify(ruleGroupRepository, never()).save(any());
    }

    @Test
    void shouldThrowInvalidRuleFieldException_whenTickerIsBlank() {
        var rules = List.of(
                ruleCommand(RuleField.PRICE, ComparisonOperator.LESS_THAN, BigDecimal.valueOf(100))
        );
        var command = new CreateRuleGroupCommand("  ", "Grupo", rules);

        assertThrows(InvalidRuleFieldException.class, () -> useCase.execute(USER_ID, command));
        verify(ruleGroupRepository, never()).save(any());
    }

    @Test
    void shouldThrowInvalidRuleFieldException_whenNameIsBlank() {
        var rules = List.of(
                ruleCommand(RuleField.PRICE, ComparisonOperator.LESS_THAN, BigDecimal.valueOf(100))
        );
        var command = new CreateRuleGroupCommand(TICKER, "", rules);

        assertThrows(InvalidRuleFieldException.class, () -> useCase.execute(USER_ID, command));
        verify(ruleGroupRepository, never()).save(any());
    }

    @Test
    void shouldThrowInvalidRuleFieldException_whenRulesListIsEmpty() {
        var command = new CreateRuleGroupCommand(TICKER, "Grupo Vazio", List.of());

        assertThrows(InvalidRuleFieldException.class, () -> useCase.execute(USER_ID, command));
        verify(ruleGroupRepository, never()).save(any());
    }

    @Test
    void shouldThrowInvalidRuleFieldException_whenRuleHasNullField() {
        var rules = List.of(
                new CreateRuleCommand(TICKER, null, ComparisonOperator.GREATER_THAN, BigDecimal.TEN, null)
        );
        var command = new CreateRuleGroupCommand(TICKER, "Grupo", rules);

        assertThrows(InvalidRuleFieldException.class, () -> useCase.execute(USER_ID, command));
        verify(ruleGroupRepository, never()).save(any());
    }

    @Test
    void shouldThrowInvalidRuleFieldException_whenRuleHasNullOperator() {
        var rules = List.of(
                new CreateRuleCommand(TICKER, RuleField.PRICE, null, BigDecimal.TEN, null)
        );
        var command = new CreateRuleGroupCommand(TICKER, "Grupo", rules);

        assertThrows(InvalidRuleFieldException.class, () -> useCase.execute(USER_ID, command));
        verify(ruleGroupRepository, never()).save(any());
    }

    @Test
    void shouldThrowInvalidRuleFieldException_whenRuleTickerDiffersFromGroupTicker() {
        var rules = List.of(
                new CreateRuleCommand("HGLG11", RuleField.PRICE, ComparisonOperator.LESS_THAN, BigDecimal.valueOf(100), null)
        );
        var command = new CreateRuleGroupCommand(TICKER, "Grupo Mismatch", rules);

        when(assetRepository.findByTicker(TICKER)).thenReturn(Optional.of(defaultAsset()));

        assertThrows(InvalidRuleFieldException.class, () -> useCase.execute(USER_ID, command));
        verify(ruleGroupRepository, never()).save(any());
    }

    @Test
    void shouldAllowMultipleGroupsForSameUser() {
        var rules1 = List.of(
                ruleCommand(RuleField.PRICE, ComparisonOperator.LESS_THAN, BigDecimal.valueOf(100))
        );
        var command1 = new CreateRuleGroupCommand(TICKER, "Grupo 1", rules1);

        var rules2 = List.of(
                ruleCommand(RuleField.DIVIDEND_YIELD, ComparisonOperator.GREATER_THAN, BigDecimal.valueOf(9))
        );
        var command2 = new CreateRuleGroupCommand(TICKER, "Grupo 2", rules2);

        when(assetRepository.findByTicker(TICKER)).thenReturn(Optional.of(defaultAsset()));
        when(ruleGroupRepository.save(any(RuleGroup.class))).thenAnswer(invocation -> {
            RuleGroup g = invocation.getArgument(0);
            g.setId(1L);
            return g;
        });

        RuleGroupResponse response1 = useCase.execute(USER_ID, command1);
        RuleGroupResponse response2 = useCase.execute(USER_ID, command2);

        assertNotNull(response1);
        assertNotNull(response2);
        verify(ruleGroupRepository, times(2)).save(any(RuleGroup.class));
    }

    @Test
    void shouldCreateRulesAsActiveByDefault() {
        var rules = List.of(
                ruleCommand(RuleField.PRICE, ComparisonOperator.LESS_THAN, BigDecimal.valueOf(100))
        );
        var command = new CreateRuleGroupCommand(TICKER, "Grupo Asset", rules);

        when(assetRepository.findByTicker(TICKER)).thenReturn(Optional.of(defaultAsset()));
        when(ruleGroupRepository.save(any(RuleGroup.class))).thenAnswer(invocation -> invocation.getArgument(0));

        useCase.execute(USER_ID, command);

        ArgumentCaptor<RuleGroup> captor = ArgumentCaptor.forClass(RuleGroup.class);
        verify(ruleGroupRepository).save(captor.capture());
        captor.getValue().getRules().forEach(rule -> assertTrue(rule.isActive()));
    }
}
