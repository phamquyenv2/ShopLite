package com.quyen.shoplite.service;

import com.quyen.shoplite.repository.UserRepository;
import com.quyen.shoplite.util.error.BadRequestException;
import com.quyen.shoplite.util.error.ResourceNotFoundException;

import com.quyen.shoplite.domain.User;
import com.quyen.shoplite.domain.request.ReqUserDTO;
import com.quyen.shoplite.domain.response.ResUserDTO;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void createUser_success() {
        ReqUserDTO req = new ReqUserDTO();
        req.setUsername("johndoe");
        req.setPassword("Password123!");

        User savedUser = User.builder()
                .id(100)
                .username("johndoe")
                .password("encoded_password")
                .isActive(true)
                .build();

        when(userRepository.existsByUsername("johndoe")).thenReturn(false);
        when(passwordEncoder.encode("Password123!")).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        ResUserDTO res = userService.create(req);

        assertNotNull(res);
        assertEquals(100, res.getId());
        assertEquals("johndoe", res.getUsername());
        verify(passwordEncoder, times(1)).encode("Password123!");
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void createUser_duplicateUsername_throwsException() {
        ReqUserDTO req = new ReqUserDTO();
        req.setUsername("johndoe");

        when(userRepository.existsByUsername("johndoe")).thenReturn(true);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> userService.create(req));
        assertTrue(ex.getMessage().contains("johndoe"));
        verify(userRepository, times(1)).existsByUsername("johndoe");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void getUserById_success() {
        User user = new User();
        user.setId(100);
        user.setUsername("johndoe");

        when(userRepository.findById(100)).thenReturn(Optional.of(user));

        ResUserDTO res = userService.findById(100);

        assertNotNull(res);
        assertEquals(100, res.getId());
        assertEquals("johndoe", res.getUsername());
    }

    @Test
    void getUserById_userNotFound_throwsException() {
        when(userRepository.findById(99)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> userService.findById(99));
        assertTrue(ex.getMessage().contains("User"));
    }

    @Test
    void updateUser_success() {
        ReqUserDTO req = new ReqUserDTO();
        req.setPassword("NewPassword!");
        req.setActive(false);

        User existingUser = new User();
        existingUser.setId(1);
        existingUser.setUsername("johndoe");

        when(userRepository.findById(1)).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.encode("NewPassword!")).thenReturn("encoded_new_pass");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResUserDTO res = userService.update(1, req);

        assertNotNull(res);
        verify(passwordEncoder, times(1)).encode("NewPassword!");
        verify(userRepository, times(1)).save(existingUser);
        assertFalse(existingUser.isActive());
        assertEquals("encoded_new_pass", existingUser.getPassword());
    }
}
