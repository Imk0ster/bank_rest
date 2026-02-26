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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardService {
    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final EncryptionUtil encryptionUtil;
    private final TransferService transferService;

    @Transactional
    public CardResponse createCard(CreateCardRequest request) {
        String encryptedNumber = encryptionUtil.encrypt(request.getCardNumber());
        if (cardRepository.existsByEncryptedNumber(encryptedNumber)) {
            throw new BadRequestException("Card number already exists");
        }

        Card card = new Card();
        card.setEncryptedNumber(encryptedNumber);
        card.setLastFourDigits(request.getCardNumber().substring(request.getCardNumber().length() - 4));
        card.setCardHolderName(request.getCardHolderName());

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/yy");
        card.setExpirationDate(LocalDate.parse("01/" + request.getExpiryDate(),
                DateTimeFormatter.ofPattern("dd/MM/yy")));

        card.setStatus("ACTIVE");
        card.setBalance(request.getInitialBalance() != null ? request.getInitialBalance() : BigDecimal.ZERO);

        return mapToResponse(cardRepository.save(card));
    }

    public Page<CardResponse> getUserCards(String username,
                                           String status,
                                           String search,
                                           Pageable pageable) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found"));

        Specification<Card> spec = (root, query, cb) ->
                cb.equal(root.get("owner"), user);

        // Фильтр по статусу
        if (status != null && !status.isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("status"), status));
        }

        if (search != null && !search.isEmpty()) {
            String searchPattern = "%" + search + "%";
            spec = spec.and((root, query, cb) ->
                    cb.or(
                            cb.like(root.get("lastFourDigits"), searchPattern),
                            cb.like(root.get("cardHolderName"), searchPattern)
                    ));
        }

        return cardRepository.findAll(spec, pageable).map(this::mapToResponse);
    }

    public CardResponse getCardBalance(String username, Long cardId) {
        Card card = getCardAndCheckOwnership(username, cardId);
        return mapToResponse(card);
    }

    @Transactional
    public CardResponse requestBlock(String username, Long cardId) {
        Card card = getCardAndCheckOwnership(username, cardId);
        card.setStatus("BLOCKED");
        return mapToResponse(cardRepository.save(card));
    }

    @Transactional
    public void transferBetweenOwnCards(String username, TransferRequest request) {
        Card fromCard = getCardAndCheckOwnership(username, request.getFromCardId());
        Card toCard = getCardAndCheckOwnership(username, request.getToCardId());

        transferService.transfer(fromCard, toCard, request.getAmount(), request.getDescription());
    }

    @Transactional
    public CardResponse adminBlockCard(Long cardId) {
        Card card = getCardById(cardId);
        card.setStatus("BLOCKED");
        return mapToResponse(cardRepository.save(card));
    }

    @Transactional
    public CardResponse adminActivateCard(Long cardId) {
        Card card = getCardById(cardId);
        card.setStatus("ACTIVE");
        return mapToResponse(cardRepository.save(card));
    }

    @Transactional
    public void adminDeleteCard(Long cardId) {
        cardRepository.deleteById(cardId);
    }

    public Page<CardResponse> getAllCards(Pageable pageable) {
        return cardRepository.findAll(pageable).map(this::mapToResponse);
    }

    private Card getCardAndCheckOwnership(String username, Long cardId) {
        Card card = getCardById(cardId);
        if (!card.getOwner().getUsername().equals(username)) {
            throw new AccessDeniedException("Card does not belong to user");
        }
        return card;
    }

    private Card getCardById(Long id) {
        return cardRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Card not found: " + id));
    }

    private CardResponse mapToResponse(Card card) {
        CardResponse response = new CardResponse();
        response.setId(card.getId());
        response.setMaskedNumber("**** **** **** " + card.getLastFourDigits());
        response.setCardHolderName(card.getCardHolderName());
        response.setExpirationDate(card.getExpirationDate().format(DateTimeFormatter.ofPattern("MM/yy")));
        response.setStatus(card.getStatus());
        response.setBalance(card.getBalance());
        return response;
    }

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void checkExpiredCards() {
        List<Card> cards = cardRepository.findAll();
        LocalDate today = LocalDate.now();

        for (Card card : cards) {
            if (card.getExpirationDate().isBefore(today) &&
                    !"EXPIRED".equals(card.getStatus())) {
                card.setStatus("EXPIRED");
                cardRepository.save(card);
                log.info("Card {} expired", card.getId());
            }
        }
    }
}