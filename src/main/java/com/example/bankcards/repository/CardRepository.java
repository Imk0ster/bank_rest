package com.example.bankcards.repository;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.List;

public interface CardRepository extends JpaRepository<Card, Long>,
        JpaSpecificationExecutor<Card> {
    Page<Card> findByOwner(User owner, Pageable pageable);
    List<Card> findByOwnerId(Long userId);
    boolean existsByEncryptedNumber(String encryptedNumber);
}