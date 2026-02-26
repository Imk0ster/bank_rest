package com.example.bankcards.service;

import com.example.bankcards.dto.CardResponse;
import com.example.bankcards.dto.CreateCardRequest;
import com.example.bankcards.dto.TransferRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.BadRequestException;
import com.example.bankcards.exception.NotFoundException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.util.EncryptionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)  // <-- ВОТ ЭТО РЕШЕНИЕ!
class CardServiceTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EncryptionUtil encryptionUtil;

    @Mock
    private TransferService transferService;

    @InjectMocks
    private CardService cardService;

    private User testUser;
    private Card testCard;
    private Card testCard2;
    private CreateCardRequest createCardRequest;
    private final String testUsername = "testuser";

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername(testUsername);
        testUser.setRole("ROLE_USER");
        testUser.setEnabled(true);

        testCard = new Card();
        testCard.setId(1L);
        testCard.setEncryptedNumber("encrypted123");
        testCard.setLastFourDigits("1234");
        testCard.setCardHolderName("Test User");
        testCard.setExpirationDate(LocalDate.now().plusYears(2));
        testCard.setStatus("ACTIVE");
        testCard.setBalance(new BigDecimal("1000.00"));
        testCard.setOwner(testUser);

        testCard2 = new Card();
        testCard2.setId(2L);
        testCard2.setEncryptedNumber("encrypted456");
        testCard2.setLastFourDigits("5678");
        testCard2.setCardHolderName("Test User");
        testCard2.setExpirationDate(LocalDate.now().plusYears(3));
        testCard2.setStatus("ACTIVE");
        testCard2.setBalance(new BigDecimal("500.00"));
        testCard2.setOwner(testUser);

        createCardRequest = new CreateCardRequest();
        createCardRequest.setCardNumber("1234567890123456");
        createCardRequest.setCardHolderName("Test User");
        createCardRequest.setExpiryDate("12/25");
        createCardRequest.setInitialBalance(new BigDecimal("1000.00"));
    }

    @Test
    void createCard_Success() {
        when(encryptionUtil.encrypt(createCardRequest.getCardNumber()))
                .thenReturn("encrypted123456");
        when(cardRepository.existsByEncryptedNumber("encrypted123456"))
                .thenReturn(false);
        when(cardRepository.save(any(Card.class)))
                .thenAnswer(invocation -> {
                    Card saved = invocation.getArgument(0);
                    saved.setId(1L);
                    return saved;
                });

        CardResponse response = cardService.createCard(createCardRequest);

        assertThat(response).isNotNull();
        assertThat(response.getMaskedNumber()).isEqualTo("**** **** **** 3456");
        assertThat(response.getBalance()).isEqualTo(new BigDecimal("1000.00"));

        verify(encryptionUtil).encrypt(createCardRequest.getCardNumber());
        verify(cardRepository).save(any(Card.class));
    }

    @Test
    void createCard_ThrowsException_WhenCardNumberAlreadyExists() {
        when(encryptionUtil.encrypt(createCardRequest.getCardNumber()))
                .thenReturn("encrypted123456");
        when(cardRepository.existsByEncryptedNumber("encrypted123456"))
                .thenReturn(true);

        assertThatThrownBy(() -> cardService.createCard(createCardRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Card number already exists");

        verify(cardRepository, never()).save(any(Card.class));
    }

    @Test
    void getUserCards_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Card> cardPage = new PageImpl<>(List.of(testCard, testCard2), pageable, 2);

        when(userRepository.findByUsername(testUsername)).thenReturn(Optional.of(testUser));
        when(cardRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(cardPage);

        Page<CardResponse> result = cardService.getUserCards(testUsername, null, null, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);

        verify(userRepository).findByUsername(testUsername);
        verify(cardRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void getUserCards_ThrowsException_WhenUserNotFound() {
        Pageable pageable = PageRequest.of(0, 10);
        when(userRepository.findByUsername(testUsername)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cardService.getUserCards(testUsername, null, null, pageable))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("User not found");
    }

    @Test
    void getCardBalance_Success() {
        when(userRepository.findByUsername(testUsername)).thenReturn(Optional.of(testUser));
        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard));

        CardResponse response = cardService.getCardBalance(testUsername, 1L);

        assertThat(response).isNotNull();
        assertThat(response.getBalance()).isEqualTo(new BigDecimal("1000.00"));
    }

    @Test
    void getCardBalance_ThrowsException_WhenCardNotFound() {
        when(userRepository.findByUsername(testUsername)).thenReturn(Optional.of(testUser));
        when(cardRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cardService.getCardBalance(testUsername, 999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Card not found: 999");
    }

    @Test
    void getCardBalance_ThrowsException_WhenCardDoesNotBelongToUser() {
        User otherUser = new User();
        otherUser.setUsername("other");
        testCard.setOwner(otherUser);

        when(userRepository.findByUsername(testUsername)).thenReturn(Optional.of(testUser));
        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard));

        assertThatThrownBy(() -> cardService.getCardBalance(testUsername, 1L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Card does not belong to user");
    }

    @Test
    void requestBlock_Success() {
        when(userRepository.findByUsername(testUsername)).thenReturn(Optional.of(testUser));
        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard));
        when(cardRepository.save(any(Card.class))).thenReturn(testCard);

        CardResponse response = cardService.requestBlock(testUsername, 1L);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("BLOCKED");

        verify(cardRepository).save(any(Card.class));
    }

    @Test
    void requestBlock_ThrowsException_WhenCardDoesNotBelongToUser() {
        User otherUser = new User();
        otherUser.setUsername("other");
        testCard.setOwner(otherUser);

        when(userRepository.findByUsername(testUsername)).thenReturn(Optional.of(testUser));
        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard));

        assertThatThrownBy(() -> cardService.requestBlock(testUsername, 1L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Card does not belong to user");
    }

    @Test
    void transferBetweenOwnCards_Success() {
        TransferRequest request = new TransferRequest();
        request.setFromCardId(1L);
        request.setToCardId(2L);
        request.setAmount(new BigDecimal("100.00"));
        request.setDescription("Test transfer");

        when(userRepository.findByUsername(testUsername)).thenReturn(Optional.of(testUser));
        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard));
        when(cardRepository.findById(2L)).thenReturn(Optional.of(testCard2));

        cardService.transferBetweenOwnCards(testUsername, request);

        verify(transferService).transfer(testCard, testCard2,
                new BigDecimal("100.00"), "Test transfer");
    }

    @Test
    void transferBetweenOwnCards_ThrowsException_WhenFromCardNotBelongToUser() {
        TransferRequest request = new TransferRequest();
        request.setFromCardId(1L);
        request.setToCardId(2L);
        request.setAmount(new BigDecimal("100.00"));

        User otherUser = new User();
        otherUser.setUsername("other");
        testCard.setOwner(otherUser);

        when(userRepository.findByUsername(testUsername)).thenReturn(Optional.of(testUser));
        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard));

        assertThatThrownBy(() -> cardService.transferBetweenOwnCards(testUsername, request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Card does not belong to user");

        verify(transferService, never()).transfer(any(), any(), any(), any());
    }

    @Test
    void adminBlockCard_Success() {
        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard));
        when(cardRepository.save(any(Card.class))).thenReturn(testCard);

        CardResponse response = cardService.adminBlockCard(1L);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("BLOCKED");
    }

    @Test
    void adminBlockCard_ThrowsException_WhenCardNotFound() {
        when(cardRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cardService.adminBlockCard(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Card not found: 999");
    }

    @Test
    void adminActivateCard_Success() {
        testCard.setStatus("BLOCKED");

        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard));
        when(cardRepository.save(any(Card.class))).thenReturn(testCard);

        CardResponse response = cardService.adminActivateCard(1L);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void adminDeleteCard_Success() {
        doNothing().when(cardRepository).deleteById(1L);

        cardService.adminDeleteCard(1L);

        verify(cardRepository).deleteById(1L);
    }

    @Test
    void getAllCards_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Card> cardPage = new PageImpl<>(List.of(testCard, testCard2), pageable, 2);

        when(cardRepository.findAll(pageable)).thenReturn(cardPage);

        Page<CardResponse> result = cardService.getAllCards(pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
    }
}