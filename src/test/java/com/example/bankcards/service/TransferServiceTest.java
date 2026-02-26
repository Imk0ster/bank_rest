package com.example.bankcards.service;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.BadRequestException;
import com.example.bankcards.repository.TransferRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock
    private TransferRepository transferRepository;

    @InjectMocks
    private TransferService transferService;

    private Card fromCard;
    private Card toCard;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");

        fromCard = new Card();
        fromCard.setId(1L);
        fromCard.setBalance(new BigDecimal("1000.00"));
        fromCard.setOwner(testUser);
        fromCard.setStatus("ACTIVE");
        fromCard.setExpirationDate(LocalDate.now().plusYears(2));

        toCard = new Card();
        toCard.setId(2L);
        toCard.setBalance(new BigDecimal("500.00"));
        toCard.setOwner(testUser);
        toCard.setStatus("ACTIVE");
        toCard.setExpirationDate(LocalDate.now().plusYears(2));
    }

    @Test
    void transfer_Success() {
        BigDecimal amount = new BigDecimal("100.00");
        String description = "Test transfer";

        transferService.transfer(fromCard, toCard, amount, description);

        assertThat(fromCard.getBalance()).isEqualTo(new BigDecimal("900.00"));
        assertThat(toCard.getBalance()).isEqualTo(new BigDecimal("600.00"));

        verify(transferRepository).save(any());
    }

    @Test
    void transfer_ThrowsException_WhenInsufficientFunds() {
        BigDecimal amount = new BigDecimal("2000.00");

        assertThatThrownBy(() -> transferService.transfer(fromCard, toCard, amount, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Insufficient funds");

        assertThat(fromCard.getBalance()).isEqualTo(new BigDecimal("1000.00"));
        assertThat(toCard.getBalance()).isEqualTo(new BigDecimal("500.00"));
    }

    @Test
    void transfer_ThrowsException_WhenTransferToSameCard() {
        BigDecimal amount = new BigDecimal("100.00");

        assertThatThrownBy(() -> transferService.transfer(fromCard, fromCard, amount, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Cannot transfer to the same card");

        assertThat(fromCard.getBalance()).isEqualTo(new BigDecimal("1000.00"));
    }

    @Test
    void transfer_WithZeroAmount_ThrowsException() {
        BigDecimal amount = BigDecimal.ZERO;

        assertThat(amount.compareTo(BigDecimal.ZERO)).isLessThanOrEqualTo(0);

        assertThatThrownBy(() -> transferService.transfer(fromCard, toCard, amount, null))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void transfer_WithNegativeAmount_ThrowsException() {
        BigDecimal amount = new BigDecimal("-50.00");

        assertThat(amount.compareTo(BigDecimal.ZERO)).isLessThan(0);

        assertThatThrownBy(() -> transferService.transfer(fromCard, toCard, amount, null))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void transfer_UpdatesBalancesCorrectly_WithMultipleTransfers() {
        transferService.transfer(fromCard, toCard, new BigDecimal("100.00"), "Transfer 1");
        assertThat(fromCard.getBalance()).isEqualTo(new BigDecimal("900.00"));
        assertThat(toCard.getBalance()).isEqualTo(new BigDecimal("600.00"));

        transferService.transfer(fromCard, toCard, new BigDecimal("200.00"), "Transfer 2");
        assertThat(fromCard.getBalance()).isEqualTo(new BigDecimal("700.00"));
        assertThat(toCard.getBalance()).isEqualTo(new BigDecimal("800.00"));

        verify(transferRepository, times(2)).save(any());
    }
}