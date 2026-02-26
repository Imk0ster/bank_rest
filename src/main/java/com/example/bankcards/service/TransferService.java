package com.example.bankcards.service;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.Transfer;
import com.example.bankcards.exception.BadRequestException;
import com.example.bankcards.repository.TransferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TransferService {
    private final TransferRepository transferRepository;

    @Transactional
    public void transfer(Card fromCard, Card toCard, BigDecimal amount, String description) {
        if (fromCard.getBalance().compareTo(amount) < 0) {
            throw new BadRequestException("Insufficient funds");
        }

        if (fromCard.getId().equals(toCard.getId())) {
            throw new BadRequestException("Cannot transfer to the same card");
        }

        fromCard.setBalance(fromCard.getBalance().subtract(amount));

        toCard.setBalance(toCard.getBalance().add(amount));

        Transfer transfer = new Transfer();
        transfer.setFromCard(fromCard);
        transfer.setToCard(toCard);
        transfer.setAmount(amount);
        transfer.setDescription(description);
        transfer.setStatus("SUCCESS");
        transfer.setCreatedAt(LocalDateTime.now());

        transferRepository.save(transfer);
    }
}