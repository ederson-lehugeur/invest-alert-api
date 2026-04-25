package com.invest.application.usecases;

import net.jqwik.api.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Validates: Requirement 1.5
 * Feature: investments-opportunity-monitor, Property 1: Round-trip de hash de senha com BCrypt
 */
class PasswordEncoderProperties {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Provide
    Arbitrary<String> validPasswords() {
        return Arbitraries.strings()
                .ofMinLength(1)
                .ofMaxLength(72)
                .ascii()
                .filter(s -> !s.isEmpty());
    }

    @Provide
    Arbitrary<String> differentPasswords() {
        return Arbitraries.strings()
                .ofMinLength(1)
                .ofMaxLength(72)
                .ascii()
                .filter(s -> !s.isEmpty());
    }

    // Feature: investments-opportunity-monitor, Property 1: Round-trip de hash de senha com BCrypt
    @Property(tries = 200)
    void hashFollowedByVerification_shouldReturnTrue(
            @ForAll("validPasswords") String password) {
        String hash = encoder.encode(password);
        assert encoder.matches(password, hash)
                : "BCrypt round-trip failed for password of length " + password.length();
    }

    // Feature: investments-opportunity-monitor, Property 1: Round-trip de hash de senha com BCrypt
    @Property(tries = 200)
    void verificationWithDifferentPassword_shouldReturnFalse(
            @ForAll("validPasswords") String original,
            @ForAll("differentPasswords") String other) {
        Assume.that(!original.equals(other));
        String hash = encoder.encode(original);
        assert !encoder.matches(other, hash)
                : "BCrypt matched different password '%s' against hash of '%s'"
                        .formatted(other, original);
    }

    // Feature: investments-opportunity-monitor, Property 1: Round-trip de hash de senha com BCrypt
    @Property(tries = 100)
    void encodingSamePassword_shouldProduceDifferentHashes(
            @ForAll("validPasswords") String password) {
        String hash1 = encoder.encode(password);
        String hash2 = encoder.encode(password);
        assert !hash1.equals(hash2)
                : "BCrypt produced identical hashes for the same password";
    }
}
