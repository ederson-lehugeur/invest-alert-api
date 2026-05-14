package com.invest.application.usecases;

import com.invest.application.commands.RegisterUserCommand;
import com.invest.application.responses.UserResponse;
import com.invest.domain.entities.Role;
import com.invest.domain.entities.User;
import com.invest.domain.exceptions.EmailAlreadyExistsException;
import com.invest.domain.ports.out.PasswordEncoder;
import com.invest.domain.ports.out.repositories.RoleRepository;
import com.invest.domain.ports.out.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegisterUserUseCaseImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RoleRepository roleRepository;

    private RegisterUserUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new RegisterUserUseCaseImpl(userRepository, passwordEncoder, roleRepository);
    }

    @Test
    void shouldRegisterUserWithValidData() {
        var command = new RegisterUserCommand("John Doe", "john@example.com", "secret123");
        var roleUser = new Role(1L, "ROLE_USER");

        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(roleUser));
        when(passwordEncoder.encode("secret123")).thenReturn("hashed_secret");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(1L);
            return u;
        });

        UserResponse response = useCase.execute(command);

        assertEquals(1L, response.id());
        assertEquals("John Doe", response.name());
        assertEquals("john@example.com", response.email());
        assertNotNull(response.createdAt());
    }

    @Test
    void shouldHashPasswordBeforeSaving() {
        var command = new RegisterUserCommand("Jane", "jane@example.com", "mypassword");
        var roleUser = new Role(1L, "ROLE_USER");

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(roleUser));
        when(passwordEncoder.encode("mypassword")).thenReturn("bcrypt_hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        useCase.execute(command);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals("bcrypt_hash", captor.getValue().getPasswordHash());
    }

    @Test
    void shouldThrowEmailAlreadyExistsException_whenEmailIsDuplicate() {
        var command = new RegisterUserCommand("John", "existing@example.com", "pass");
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        assertThrows(EmailAlreadyExistsException.class, () -> useCase.execute(command));
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldThrowIllegalStateException_whenRoleUserNotFound() {
        var command = new RegisterUserCommand("John", "john@example.com", "pass");
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> useCase.execute(command));
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldAssignRoleUserOnSuccessfulRegistration() {
        var command = new RegisterUserCommand("John", "john@example.com", "pass");
        var roleUser = new Role(1L, "ROLE_USER");

        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(roleUser));
        when(passwordEncoder.encode("pass")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = useCase.execute(command);

        assertTrue(response.roles().contains("ROLE_USER"),
                "UserResponse must contain ROLE_USER after registration");
    }
}
