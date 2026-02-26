package com.example.bankcards.service;

import com.example.bankcards.dto.UserRequest;
import com.example.bankcards.dto.UserResponse;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.BadRequestException;
import com.example.bankcards.exception.NotFoundException;
import com.example.bankcards.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminService adminService;

    private User testUser;
    private UserRequest userRequest;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setPassword("encodedPassword");
        testUser.setEmail("test@test.com");
        testUser.setRole("ROLE_USER");
        testUser.setEnabled(true);
        testUser.setCreatedAt(LocalDateTime.now());

        userRequest = new UserRequest();
        userRequest.setUsername("newuser");
        userRequest.setPassword("password123");
        userRequest.setEmail("new@test.com");
        userRequest.setRole("ROLE_USER");
    }

    @Test
    void getAllUsers_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> userPage = new PageImpl<>(List.of(testUser), pageable, 1);

        when(userRepository.findAll(pageable)).thenReturn(userPage);

        Page<UserResponse> result = adminService.getAllUsers(pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getUsername()).isEqualTo("testuser");
    }

    @Test
    void getUserById_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        UserResponse result = adminService.getUserById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("testuser");
        assertThat(result.getEmail()).isEqualTo("test@test.com");
    }

    @Test
    void getUserById_ThrowsException_WhenNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.getUserById(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("User not found: 999");
    }

    @Test
    void createUser_Success() {
        when(userRepository.findByUsername(userRequest.getUsername())).thenReturn(Optional.empty());
        when(userRepository.findByEmail(userRequest.getEmail())).thenReturn(null);
        when(passwordEncoder.encode(userRequest.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(2L);
            return saved;
        });

        UserResponse result = adminService.createUser(userRequest);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(2L);
        assertThat(result.getUsername()).isEqualTo("newuser");
        assertThat(result.getEmail()).isEqualTo("new@test.com");

        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUser_ThrowsException_WhenUsernameExists() {
        when(userRepository.findByUsername(userRequest.getUsername())).thenReturn(Optional.of(testUser));

        assertThatThrownBy(() -> adminService.createUser(userRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Username already exists: newuser");

        verify(userRepository, never()).save(any());
    }

    @Test
    void createUser_ThrowsException_WhenEmailExists() {
        when(userRepository.findByUsername(userRequest.getUsername())).thenReturn(Optional.empty());
        when(userRepository.findByEmail(userRequest.getEmail())).thenReturn(testUser);

        assertThatThrownBy(() -> adminService.createUser(userRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Email already exists: new@test.com");

        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUser_Success() {
        UserRequest updateRequest = new UserRequest();
        updateRequest.setUsername("updated");
        updateRequest.setPassword("newpassword123");
        updateRequest.setEmail("updated@test.com");
        updateRequest.setRole("ROLE_ADMIN");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.findByUsername("updated")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("updated@test.com")).thenReturn(null);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        UserResponse result = adminService.updateUser(1L, updateRequest);

        assertThat(result).isNotNull();
        verify(userRepository).save(any(User.class));
    }

    @Test
    void toggleUserStatus_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        UserResponse result = adminService.toggleUserStatus(1L, false);

        assertThat(result).isNotNull();
        assertThat(result.getEnabled()).isFalse();
        verify(userRepository).save(any(User.class));
    }

    @Test
    void changeUserRole_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        UserResponse result = adminService.changeUserRole(1L, "ADMIN");

        assertThat(result).isNotNull();
        assertThat(result.getRole()).isEqualTo("ROLE_ADMIN");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void changeUserRole_ThrowsException_WhenInvalidRole() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        assertThatThrownBy(() -> adminService.changeUserRole(1L, "INVALID"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Invalid role. Use ADMIN or USER");

        verify(userRepository, never()).save(any());
    }
}