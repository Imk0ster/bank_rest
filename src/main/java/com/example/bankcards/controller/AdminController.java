package com.example.bankcards.controller;

import com.example.bankcards.dto.CardResponse;
import com.example.bankcards.dto.CreateCardRequest;
import com.example.bankcards.dto.UserRequest;
import com.example.bankcards.dto.UserResponse;
import com.example.bankcards.service.AdminService;
import com.example.bankcards.service.CardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Управление пользователями и картами (только для ADMIN)")
public class AdminController {
    private final AdminService adminService;
    private final CardService cardService;

    @GetMapping("/users")
    @Operation(summary = "Получить всех пользователей (с пагинацией)")
    public ResponseEntity<Page<UserResponse>> getAllUsers(Pageable pageable) {
        return ResponseEntity.ok(adminService.getAllUsers(pageable));
    }

    @GetMapping("/users/{id}")
    @Operation(summary = "Получить пользователя по ID")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.getUserById(id));
    }

    @PostMapping("/users")
    @Operation(summary = "Создать нового пользователя")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createUser(request));
    }

    @PutMapping("/users/{id}")
    @Operation(summary = "Обновить данные пользователя")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserRequest request) {
        return ResponseEntity.ok(adminService.updateUser(id, request));
    }

    @PatchMapping("/users/{id}/status")
    @Operation(summary = "Заблокировать/разблокировать пользователя")
    public ResponseEntity<UserResponse> toggleUserStatus(
            @PathVariable Long id,
            @RequestParam Boolean enabled) {
        return ResponseEntity.ok(adminService.toggleUserStatus(id, enabled));
    }

    @PatchMapping("/users/{id}/role")
    @Operation(summary = "Изменить роль пользователя")
    public ResponseEntity<UserResponse> changeUserRole(
            @PathVariable Long id,
            @RequestParam String role) {
        return ResponseEntity.ok(adminService.changeUserRole(id, role));
    }

    @GetMapping("/cards")
    @Operation(summary = "Видеть все карты (ADMIN)")
    public ResponseEntity<Page<CardResponse>> getAllCards(Pageable pageable) {
        return ResponseEntity.ok(cardService.getAllCards(pageable));
    }

    @PostMapping("/cards")
    @Operation(summary = "Создать карту (ADMIN)")
    public ResponseEntity<CardResponse> createCard(@Valid @RequestBody CreateCardRequest request) {
        return ResponseEntity.ok(cardService.createCard(request));
    }

    @PutMapping("/cards/{id}/block")
    @Operation(summary = "Заблокировать карту (ADMIN)")
    public ResponseEntity<CardResponse> blockCard(@PathVariable Long id) {
        return ResponseEntity.ok(cardService.adminBlockCard(id));
    }

    @PutMapping("/cards/{id}/activate")
    @Operation(summary = "Активировать карту (ADMIN)")
    public ResponseEntity<CardResponse> activateCard(@PathVariable Long id) {
        return ResponseEntity.ok(cardService.adminActivateCard(id));
    }

    @DeleteMapping("/cards/{id}")
    @Operation(summary = "Удалить карту (ADMIN)")
    public ResponseEntity<Void> deleteCard(@PathVariable Long id) {
        cardService.adminDeleteCard(id);
        return ResponseEntity.noContent().build();
    }
}