package com.example.bankcards.controller;

import com.example.bankcards.dto.CardResponse;
import com.example.bankcards.dto.CreateCardRequest;
import com.example.bankcards.dto.UserRequest;
import com.example.bankcards.dto.UserResponse;
import com.example.bankcards.service.AdminService;
import com.example.bankcards.service.CardService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AdminService adminService;

    @MockBean
    private CardService cardService;

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void getAllUsers_Success() throws Exception {
        UserResponse user1 = new UserResponse();
        user1.setId(1L);
        user1.setUsername("user1");
        user1.setEmail("user1@test.com");
        user1.setRole("ROLE_USER");
        user1.setEnabled(true);
        user1.setCardsCount(2L);

        UserResponse user2 = new UserResponse();
        user2.setId(2L);
        user2.setUsername("user2");
        user2.setEmail("user2@test.com");
        user2.setRole("ROLE_ADMIN");
        user2.setEnabled(true);
        user2.setCardsCount(0L);

        PageImpl<UserResponse> page = new PageImpl<>(
                List.of(user1, user2),
                PageRequest.of(0, 20),
                2
        );

        when(adminService.getAllUsers(any())).thenReturn(page);

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].username").value("user1"))
                .andExpect(jsonPath("$.content[1].username").value("user2"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void getUserById_Success() throws Exception {
        UserResponse user = new UserResponse();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("test@test.com");
        user.setRole("ROLE_USER");
        user.setEnabled(true);

        when(adminService.getUserById(1L)).thenReturn(user);

        mockMvc.perform(get("/api/admin/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@test.com"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void createUser_Success() throws Exception {
        UserRequest request = new UserRequest();
        request.setUsername("newuser");
        request.setPassword("password123");
        request.setEmail("new@test.com");
        request.setRole("ROLE_USER");

        UserResponse response = new UserResponse();
        response.setId(3L);
        response.setUsername("newuser");
        response.setEmail("new@test.com");
        response.setRole("ROLE_USER");
        response.setEnabled(true);

        when(adminService.createUser(any(UserRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(3L))
                .andExpect(jsonPath("$.username").value("newuser"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void updateUser_Success() throws Exception {
        UserRequest request = new UserRequest();
        request.setUsername("updateduser");
        request.setPassword("newpassword123");
        request.setEmail("updated@test.com");
        request.setRole("ROLE_ADMIN");

        UserResponse response = new UserResponse();
        response.setId(1L);
        response.setUsername("updateduser");
        response.setEmail("updated@test.com");
        response.setRole("ROLE_ADMIN");

        when(adminService.updateUser(eq(1L), any(UserRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/admin/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("updateduser"))
                .andExpect(jsonPath("$.role").value("ROLE_ADMIN"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void toggleUserStatus_Success() throws Exception {
        UserResponse response = new UserResponse();
        response.setId(1L);
        response.setEnabled(false);

        when(adminService.toggleUserStatus(1L, false)).thenReturn(response);

        mockMvc.perform(patch("/api/admin/users/1/status")
                        .param("enabled", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void changeUserRole_Success() throws Exception {
        UserResponse response = new UserResponse();
        response.setId(1L);
        response.setRole("ROLE_ADMIN");

        when(adminService.changeUserRole(1L, "ADMIN")).thenReturn(response);

        mockMvc.perform(patch("/api/admin/users/1/role")
                        .param("role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ROLE_ADMIN"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void getAllCards_Success() throws Exception {
        CardResponse card1 = new CardResponse();
        card1.setId(1L);
        card1.setMaskedNumber("**** **** **** 1234");

        CardResponse card2 = new CardResponse();
        card2.setId(2L);
        card2.setMaskedNumber("**** **** **** 5678");

        PageImpl<CardResponse> page = new PageImpl<>(
                List.of(card1, card2),
                PageRequest.of(0, 20),
                2
        );

        when(cardService.getAllCards(any())).thenReturn(page);

        mockMvc.perform(get("/api/admin/cards"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void createCard_Success() throws Exception {
        CreateCardRequest request = new CreateCardRequest();
        request.setCardNumber("1234567890123456");
        request.setCardHolderName("Test User");
        request.setExpiryDate("12/25");
        request.setInitialBalance(new BigDecimal("1000.00"));

        CardResponse response = new CardResponse();
        response.setId(1L);
        response.setMaskedNumber("**** **** **** 3456");
        response.setCardHolderName("Test User");
        response.setBalance(new BigDecimal("1000.00"));

        when(cardService.createCard(any(CreateCardRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/admin/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.maskedNumber").value("**** **** **** 3456"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void blockCard_Success() throws Exception {
        CardResponse response = new CardResponse();
        response.setId(1L);
        response.setStatus("BLOCKED");

        when(cardService.adminBlockCard(1L)).thenReturn(response);

        mockMvc.perform(put("/api/admin/cards/1/block"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BLOCKED"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void activateCard_Success() throws Exception {
        CardResponse response = new CardResponse();
        response.setId(1L);
        response.setStatus("ACTIVE");

        when(cardService.adminActivateCard(1L)).thenReturn(response);

        mockMvc.perform(put("/api/admin/cards/1/activate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void deleteCard_Success() throws Exception {
        doNothing().when(cardService).adminDeleteCard(1L);

        mockMvc.perform(delete("/api/admin/cards/1"))
                .andExpect(status().isNoContent());

        verify(cardService).adminDeleteCard(1L);
    }

    @Test
    @WithMockUser(username = "user", roles = "USER")
    void adminEndpoints_WithUserRole_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/cards"))
                .andExpect(status().isForbidden());
    }
}