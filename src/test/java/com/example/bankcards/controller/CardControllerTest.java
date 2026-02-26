package com.example.bankcards.controller;

import com.example.bankcards.dto.CardResponse;
import com.example.bankcards.dto.TransferRequest;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CardService cardService;

    @Test
    @WithMockUser(username = "user")
    void getMyCards_Success() throws Exception {
        CardResponse card1 = new CardResponse();
        card1.setId(1L);
        card1.setMaskedNumber("**** **** **** 1234");
        card1.setBalance(new BigDecimal("1000.00"));

        CardResponse card2 = new CardResponse();
        card2.setId(2L);
        card2.setMaskedNumber("**** **** **** 5678");
        card2.setBalance(new BigDecimal("500.00"));

        PageImpl<CardResponse> page = new PageImpl<>(
                List.of(card1, card2),
                PageRequest.of(0, 20),
                2
        );

        when(cardService.getUserCards(eq("user"), isNull(), isNull(), any()))
                .thenReturn(page);

        mockMvc.perform(get("/api/cards/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].maskedNumber").value("**** **** **** 1234"))
                .andExpect(jsonPath("$.content[1].maskedNumber").value("**** **** **** 5678"));
    }

    @Test
    @WithMockUser(username = "user")
    void getMyCards_WithFilters_Success() throws Exception {
        CardResponse card = new CardResponse();
        card.setId(1L);
        card.setMaskedNumber("**** **** **** 1234");
        card.setStatus("ACTIVE");

        PageImpl<CardResponse> page = new PageImpl<>(
                List.of(card),
                PageRequest.of(0, 20),
                1
        );

        when(cardService.getUserCards(eq("user"), eq("ACTIVE"), eq("1234"), any()))
                .thenReturn(page);

        mockMvc.perform(get("/api/cards/my")
                        .param("status", "ACTIVE")
                        .param("search", "1234"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("ACTIVE"));
    }

    @Test
    @WithMockUser(username = "user")
    void getCardBalance_Success() throws Exception {
        CardResponse response = new CardResponse();
        response.setId(1L);
        response.setBalance(new BigDecimal("1000.00"));

        when(cardService.getCardBalance("user", 1L)).thenReturn(response);

        mockMvc.perform(get("/api/cards/my/1/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(1000.00));
    }

    @Test
    @WithMockUser(username = "user")
    void requestBlock_Success() throws Exception {
        CardResponse response = new CardResponse();
        response.setId(1L);
        response.setStatus("BLOCKED");

        when(cardService.requestBlock("user", 1L)).thenReturn(response);

        mockMvc.perform(post("/api/cards/my/1/block"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BLOCKED"));
    }

    @Test
    @WithMockUser(username = "user")
    void transfer_Success() throws Exception {
        TransferRequest request = new TransferRequest();
        request.setFromCardId(1L);
        request.setToCardId(2L);
        request.setAmount(new BigDecimal("100.00"));
        request.setDescription("Test transfer");

        doNothing().when(cardService).transferBetweenOwnCards(eq("user"), any(TransferRequest.class));

        mockMvc.perform(post("/api/cards/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(cardService).transferBetweenOwnCards(eq("user"), any(TransferRequest.class));
    }

    @Test
    void transfer_WithoutAuth_ReturnsUnauthorized() throws Exception {
        TransferRequest request = new TransferRequest();
        request.setFromCardId(1L);
        request.setToCardId(2L);
        request.setAmount(new BigDecimal("100.00"));

        mockMvc.perform(post("/api/cards/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}