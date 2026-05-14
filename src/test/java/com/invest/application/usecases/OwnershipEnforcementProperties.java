package com.invest.application.usecases;

import com.invest.application.commands.UpdateRuleCommand;
import com.invest.domain.entities.Rule;
import com.invest.domain.entities.enumerator.ComparisonOperator;
import com.invest.domain.entities.enumerator.RuleField;
import com.invest.domain.exceptions.AccessDeniedException;
import com.invest.domain.exceptions.RuleNotFoundException;
import com.invest.domain.ports.out.repositories.AlertRepository;
import com.invest.domain.ports.out.repositories.RuleRepository;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tuple;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

/**
 * Property 9: Ownership violation always returns 403, never 404.
 * For any resource owned by user A, when user B (where B != A) attempts to access
 * or mutate that resource, the response must be HTTP 403 Forbidden - never HTTP 404.
 * Validates: Requirements 6.1, 6.2, 6.5
 */
class OwnershipEnforcementProperties {

    private final RuleRepository ruleRepository = mock(RuleRepository.class);
    private final AlertRepository alertRepository = mock(AlertRepository.class);
    private final DeleteRuleUseCaseImpl deleteUseCase =
            new DeleteRuleUseCaseImpl(ruleRepository, alertRepository);
    private final UpdateRuleUseCaseImpl updateUseCase =
            new UpdateRuleUseCaseImpl(ruleRepository, alertRepository);

    /**
     * Property 9a: Delete ownership violation always throws AccessDeniedException, never RuleNotFoundException.
     * When a rule exists but belongs to a different user, deleting it must throw AccessDeniedException (403),
     * not RuleNotFoundException (404).
     */
    @Property(tries = 100)
    void deleteOwnershipViolationAlwaysThrowsAccessDenied(
            @ForAll("differentUserIds") Tuple.Tuple2<Long, Long> ownerAndAttacker,
            @ForAll("validRuleIds") Long ruleId) {

        Long ownerId = ownerAndAttacker.get1();
        Long attackerId = ownerAndAttacker.get2();

        Rule rule = new Rule(ruleId, ownerId, "XPLG11", null,
                RuleField.PRICE, ComparisonOperator.GREATER_THAN, BigDecimal.valueOf(100),
                true, LocalDateTime.now(), LocalDateTime.now());

        reset(ruleRepository);
        when(ruleRepository.findById(ruleId)).thenReturn(Optional.of(rule));

        assertThrows(
                AccessDeniedException.class,
                () -> deleteUseCase.execute(attackerId, ruleId),
                "Ownership violation must throw AccessDeniedException (403), not RuleNotFoundException (404)"
        );
    }

    /**
     * Property 9b: Update ownership violation always throws AccessDeniedException, never RuleNotFoundException.
     */
    @Property(tries = 100)
    void updateOwnershipViolationAlwaysThrowsAccessDenied(
            @ForAll("differentUserIds") Tuple.Tuple2<Long, Long> ownerAndAttacker,
            @ForAll("validRuleIds") Long ruleId) {

        Long ownerId = ownerAndAttacker.get1();
        Long attackerId = ownerAndAttacker.get2();

        Rule rule = new Rule(ruleId, ownerId, "XPLG11", null,
                RuleField.PRICE, ComparisonOperator.GREATER_THAN, BigDecimal.valueOf(100),
                true, LocalDateTime.now(), LocalDateTime.now());

        reset(ruleRepository);
        when(ruleRepository.findById(ruleId)).thenReturn(Optional.of(rule));

        var command = new UpdateRuleCommand(RuleField.PRICE, ComparisonOperator.GREATER_THAN, BigDecimal.TEN);

        assertThrows(
                AccessDeniedException.class,
                () -> updateUseCase.execute(attackerId, ruleId, command),
                "Ownership violation must throw AccessDeniedException (403), not RuleNotFoundException (404)"
        );
    }

    /**
     * Property 9c: Non-existent rule always throws RuleNotFoundException, never AccessDeniedException.
     * When a rule does not exist at all, the response must be 404, not 403.
     */
    @Property(tries = 100)
    void nonExistentRuleAlwaysThrowsRuleNotFoundException(
            @ForAll("validRuleIds") Long userId,
            @ForAll("validRuleIds") Long ruleId) {

        reset(ruleRepository);
        when(ruleRepository.findById(ruleId)).thenReturn(Optional.empty());

        assertThrows(
                RuleNotFoundException.class,
                () -> deleteUseCase.execute(userId, ruleId),
                "Non-existent rule must throw RuleNotFoundException (404), not AccessDeniedException (403)"
        );
    }

    @Provide
    Arbitrary<Tuple.Tuple2<Long, Long>> differentUserIds() {
        return Arbitraries.longs().between(1L, 1000L).flatMap(ownerId ->
                Arbitraries.longs().between(1L, 1000L)
                        .filter(attackerId -> !attackerId.equals(ownerId))
                        .map(attackerId -> Tuple.of(ownerId, attackerId))
        );
    }

    @Provide
    Arbitrary<Long> validRuleIds() {
        return Arbitraries.longs().between(1L, Long.MAX_VALUE);
    }
}
