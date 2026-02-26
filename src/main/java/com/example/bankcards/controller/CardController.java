package com.example.bankcards.controller;

import com.example.bankcards.dto.CardResponse;
import com.example.bankcards.dto.CreateCardRequest;
import com.example.bankcards.dto.TransferRequest;
import com.example.bankcards.service.CardService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
@Tag(name = "Cards", description = "Операции с картами для пользователей")
public class CardController {
    private final CardService cardService;

    @GetMapping("/my")
    @Operation(summary = "Просмотр своих карт (с фильтрацией и пагинацией)")
    public ResponseEntity<Page<CardResponse>> getMyCards(
            Authentication authentication,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            Pageable pageable) {
        return ResponseEntity.ok(cardService.getUserCards(
                authentication.getName(),
                status,
                search,
                pageable));
    }

    @GetMapping("/my/{id}/balance")
    @Operation(summary = "Посмотреть баланс карты")
    public ResponseEntity<CardResponse> getCardBalance(
            Authentication authentication,
            @PathVariable Long id) {
        return ResponseEntity.ok(cardService.getCardBalance(authentication.getName(), id));
    }

    @PostMapping("/my/{id}/block")
    @Operation(summary = "Запросить блокировку карты")
    public ResponseEntity<CardResponse> requestBlock(
            Authentication authentication,
            @PathVariable Long id) {
        return ResponseEntity.ok(cardService.requestBlock(authentication.getName(), id));
    }

    @PostMapping("/transfer")
    @Operation(summary = "Перевод между своими картами")
    public ResponseEntity<?> transfer(
            Authentication authentication,
            @Valid @RequestBody TransferRequest request) {
        cardService.transferBetweenOwnCards(authentication.getName(), request);
        return ResponseEntity.ok().build();
    }
}